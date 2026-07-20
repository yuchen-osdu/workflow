package org.opengroup.osdu.azure.workflow.framework.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.azure.workflow.framework.consts.DefaultVariable;
import org.opengroup.osdu.azure.workflow.framework.models.WorkflowMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TestDataUtil {
  public static final String DAGS_SECTION_KEY = "dags";
  public static final String OPERATORS_SECTION_KEY = "operators";

  private static JsonObject testData;
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public static JsonObject getWorkflow(String dagName) {
    return getTestData().getAsJsonObject(DAGS_SECTION_KEY).getAsJsonObject(dagName);
  }

  public static List<String> getAllWorkflowIds() {
    List<String> workflowIds = new ArrayList<>();

    JsonObject workflows = getTestData().getAsJsonObject(DAGS_SECTION_KEY);
    for(String workflowName: workflows.keySet()) {
      workflowIds.add(workflows.get(workflowName).getAsJsonObject().get("workflowId").getAsString());
    }
    return workflowIds;
  }

  public static JsonObject getAllOperators() {
    return getTestData().getAsJsonObject(OPERATORS_SECTION_KEY);
  }

  public static Map<String, WorkflowMetadata> getAllWorkflows() {
    Map<String, WorkflowMetadata> testDataWorkflowNameToInfo = new HashMap<>();
    JsonObject workflows = getTestData().getAsJsonObject(DAGS_SECTION_KEY);
    for (String workflowMetadataKey : workflows.keySet()) {
      WorkflowMetadata workflowMetadata = TestBase.gson.fromJson(
          workflows.get(workflowMetadataKey), WorkflowMetadata.class);
      testDataWorkflowNameToInfo.put(workflowMetadata.getWorkflowName(),
          workflowMetadata);
    }
    return testDataWorkflowNameToInfo;
  }

  public static JsonObject getOperator(String operatorName) {
    return getTestData().getAsJsonObject(OPERATORS_SECTION_KEY).getAsJsonObject(operatorName);
  }

  private static JsonObject getTestData() {
    try {
      if(testData == null) {
        Path path = getTestDataPath();
        String testDataString = new String(Files.readAllBytes(path));
        testData = gson.fromJson(testDataString, JsonObject.class);
      }
      return testData;
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static void saveTestData(Map<String, Object> dagData, Map<String, Object> operatorsData)
      throws Exception {
    Map<String, Object> testData = new HashMap<>();
    testData.put(DAGS_SECTION_KEY, dagData);
    testData.put(OPERATORS_SECTION_KEY, operatorsData);

    String contentToSave = gson.toJson(testData);
    Path testDataPath = getTestDataPath();

    log.info("Creating test data at {}", testDataPath.toString());
    Files.write(testDataPath, contentToSave.getBytes());
  }

  private static Path getTestDataPath() {
    String testDataDirectory = DefaultVariable
        .getEnvironmentVariableOrDefaultKey(DefaultVariable.TEST_DATA_DIRECTORY);
    String fileName = DefaultVariable
        .getEnvironmentVariableOrDefaultKey(DefaultVariable.TEST_DATA_FILE_NAME);
    return Paths.get(testDataDirectory, fileName);
  }

}
