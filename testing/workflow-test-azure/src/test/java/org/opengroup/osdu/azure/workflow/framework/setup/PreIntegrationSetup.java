package org.opengroup.osdu.azure.workflow.framework.setup;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder;
import org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder;
import org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorUtil;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.TestDataUtil;
import org.opengroup.osdu.azure.workflow.framework.util.TestResourceProvider;
import org.opengroup.osdu.azure.workflow.framework.util.WorkflowUtil;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.CUSTOM_OPERATOR_URL;

@Slf4j
public class PreIntegrationSetup {
  private static final String MANIFEST_FILE_NAME = "manifest.json";
  private static final String FILE_NAME_KEY = "fileName";
  private static final String DAG_MANIFEST_KEY = "dags";
  private static final String OPERATOR_MANIFEST_KEY = "operators";
  private static final String OPERATOR_CLASS_NAME_KEY = "className";
  private static final String OPERATOR_PROPERTIES_KEY = "properties";

  private final HTTPClient client;
  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public PreIntegrationSetup(HTTPClient client) {
    this.client = client;
  }

  public void run() {
    try {
      Map<String, Object> manifest = getManifest();
      Map<String, Object> operatorsData =
          createOperators((Map<String, Object>)manifest.get(OPERATOR_MANIFEST_KEY));
      Map<String, Object> dagData =
          createWorkflows((Map<String, Object>)manifest.get(DAG_MANIFEST_KEY), operatorsData);
      TestDataUtil.saveTestData(dagData, operatorsData);
    } catch (Exception e) {
      throw new RuntimeException("Setup step failed", e);
    }
  }

  private Map<String, Object> getManifest() throws IOException  {
    InputStream manifestFileInputStream = PreIntegrationSetup.class.getClassLoader().
        getResourceAsStream(MANIFEST_FILE_NAME);
    String manifestFileContent = CharStreams
        .toString(new InputStreamReader(manifestFileInputStream));
    return gson.fromJson(manifestFileContent, Map.class);
  }

  private Map<String, Object> createOperators(Map<String, Object> operatorsManifest)
      throws Exception {
    Map<String, Object> operatorsData = new HashMap<>();
    for(Map.Entry<String, Object> operatorManifestEntry: operatorsManifest.entrySet()) {
      String operatorName = operatorManifestEntry.getKey();
      String operatorUniqueName = CustomOperatorUtil.getUniqueOperatorName(operatorName);
      Map<String, Object> operatorProperties =
          (Map<String, Object>) operatorManifestEntry.getValue();
      log.info("Creating custom operator with name {}", operatorUniqueName);
      Map<String, Object> customOperatorData = callRegisterCustomOperatorApi(operatorUniqueName,
          (String) operatorProperties.get(OPERATOR_CLASS_NAME_KEY), TestResourceProvider
              .getOperatorFileContent((String) operatorProperties.get(FILE_NAME_KEY)),
          (List<Map<String, Object>>)operatorProperties.get(OPERATOR_PROPERTIES_KEY));
      log.info("Creating custom operator with name {} successful", operatorUniqueName);
      operatorsData.put(operatorName, customOperatorData);
    }
    return operatorsData;
  }

  private Map<String, Object> createWorkflows(Map<String, Object> dagManifest,
                                              Map<String, Object> operatorsData) throws Exception {
    Map<String, Object> dagData = new HashMap<>();
    for(Map.Entry<String, Object> dagManifestEntry: dagManifest.entrySet()) {
      String dagName = dagManifestEntry.getKey();
      Map<String, Object> dagProperties = (Map<String, Object>) dagManifestEntry.getValue();
      String workflowName = WorkflowUtil.getUniqueWorkflowName(dagName);
      String dagContent = getDAGContentFromTemplate((String) dagProperties.get(FILE_NAME_KEY),
          workflowName, operatorsData);
      log.info("Creating workflow with name {}", workflowName);
      Map<String, Object> dagCreationData = callCreateWorkFlowApi(workflowName, dagContent);
      log.info("Creating workflow with name {} successful", workflowName);
      dagData.put(dagName, dagCreationData);
    }
    WorkflowUtil.waitForDAGActivation();
    return dagData;
  }

  private String getDAGContentFromTemplate(String dagTemplateName, String dagName,
                                           Map<String, Object> operatorsData) throws Exception {
    Map<String, Object> dagTemplateContext = new HashMap<>();
    dagTemplateContext.put("dagId", dagName);
    dagTemplateContext.put("operators", operatorsData);
    return TestResourceProvider.getDAGFileContent(dagTemplateName, dagTemplateContext);
  }

  private Map<String, Object> callCreateWorkFlowApi(String workflowName, String dagContent)
      throws Exception {
    Map<String, Object> createWorkFlowData = CreateWorkflowTestsBuilder
        .buildCreateWorkflowRequest(workflowName, dagContent);
    return callPostApi(CREATE_WORKFLOW_URL, createWorkFlowData);
  }

  private Map<String, Object> callRegisterCustomOperatorApi(
      String operatorName, String operatorClassName, String operatorContent,
      List<Map<String, Object>> operatorProperties) throws Exception {
    Map<String, Object> customOperatorData = CustomOperatorTestsBuilder
        .buildRegisterCustomOperatorPayload(operatorName, operatorClassName, operatorContent,
            operatorProperties);
    return callPostApi(CUSTOM_OPERATOR_URL, customOperatorData);
  }

  private Map<String, Object> callPostApi(String endpoint, Map<String, Object> payload)
      throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        endpoint,
        gson.toJson(payload),
        client.getCommonHeader(),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    return gson.fromJson(response.getEntity(String.class), Map.class);
  }
}
