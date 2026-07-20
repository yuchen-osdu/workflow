package org.opengroup.osdu.azure.workflow.framework.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.AzureTestBase;
import org.springframework.http.HttpStatus;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_ACTIVE;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_CONCURRENT_TASK_RUN;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_CONCURRENT_WORKFLOW_RUN;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_DESCRIPTION;
import static org.opengroup.osdu.azure.workflow.framework.util.CreateWorkflowTestsBuilder.WORKFLOW_DESCRIPTION_FIELD;
import static org.opengroup.osdu.azure.workflow.utils.AzurePayLoadBuilder.buildCreateWorkflowRequestWithRegistrationInstructions;
import static org.opengroup.osdu.azure.workflow.utils.AzurePayLoadBuilder.buildCreateWorkflowValidPayloadWithDagContent;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

public abstract class PostCreateWorkflowIntegrationTests extends AzureTestBase {

  private static final String INVALID_PREFIX = "backfill";
  private static final Integer INVALID_LIMIT = 1000;
  public static final Gson gson = new Gson();

  @Test
  public void shouldReturnForbidden_whenGivenDagContent_withIgnoreDagContentAsTrue() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowValidPayloadWithDagContent(),
        headers,
        client.getAccessToken()
    );
    assertEquals(org.springframework.http.HttpStatus.FORBIDDEN.value(), response.getStatus());
  }

  @Test
  public void should_returnBadRequest_when_givenInvalidPrefix() throws Exception {
    String workflowResponseBody = createWorkflowWithRegistrationInstructions();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    assertEquals(workflowInfo.get(WORKFLOW_DESCRIPTION_FIELD), WORKFLOW_DESCRIPTION);
    assertEquals(workflowInfo.get(WORKFLOW_ID_FIELD), CREATE_WORKFLOW_WORKFLOW_NAME);
    assertEquals(workflowInfo.get(WORKFLOW_NAME_FIELD), CREATE_WORKFLOW_WORKFLOW_NAME);

    Map<String, String> registrationInstructions = new ObjectMapper().readValue(new Gson().toJson(workflowInfo.get("registrationInstructions")), HashMap.class);

    assertEquals(registrationInstructions.get("dagName"), CREATE_WORKFLOW_WORKFLOW_NAME);
    assertEquals(registrationInstructions.get("active"), WORKFLOW_ACTIVE);
    assertEquals(registrationInstructions.get("concurrentWorkflowRun"), WORKFLOW_CONCURRENT_WORKFLOW_RUN);
    assertEquals(registrationInstructions.get("concurrentTaskRun"), WORKFLOW_CONCURRENT_TASK_RUN);
  }

  private String createWorkflowWithRegistrationInstructions() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        new Gson().toJson(buildCreateWorkflowRequestWithRegistrationInstructions(CREATE_WORKFLOW_WORKFLOW_NAME)),
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.OK.value(), response.getStatus(), response.toString());
    return response.getEntity(String.class);
  }
}
