package org.opengroup.osdu.azure.workflow.framework.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.AzureTestBase;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.azure.workflow.utils.AzurePayLoadBuilder.buildCreateWorkflowValidPayloadWithGivenDescription;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_URL;

public abstract class DeleteWorkflowIntegrationTests extends AzureTestBase {

  @Test
  public void should_throw_error_when_given_valid_workflow_id_with_active_runs() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse deleteResponse = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, CREATE_WORKFLOW_WORKFLOW_NAME),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_PRECONDITION_FAILED, deleteResponse.getStatus());
  }

  /* The below test checks the caching functionality added for caching workflow metadata */
  @Test
  public void should_get_latest_workflow_details_when_old_workflow_is_deleted() throws Exception {
    // Step 1: Create a workflow with description "test-description-1"
    String workflowDescription = "test-description-1";
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowValidPayloadWithGivenDescription(workflowDescription),
        headers,
        client.getAccessToken()
    );
    assertEquals(org.springframework.http.HttpStatus.OK.value(), response.getStatus(), response.toString());

    // Step 2: Delete the workflow created with description "test-description-1"
    ClientResponse deleteResponse = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, CREATE_WORKFLOW_WORKFLOW_NAME),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NO_CONTENT, deleteResponse.getStatus());

    // Step 3: Create a workflow with description "test-description-2"
    workflowDescription = "test-description-2";
    response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowValidPayloadWithGivenDescription(workflowDescription),
        headers,
        client.getAccessToken()
    );
    String responseBody = response.getEntity(String.class);
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    // Step 4: Get workflow details and ensure that the old workflow is successfully deleted from cache as well by verifying the description of the latest workflow
    response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_URL, CREATE_WORKFLOW_WORKFLOW_NAME),
        null,
        headers,
        client.getAccessToken()
    );
    responseBody = response.getEntity(String.class);
    workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);

    assertEquals(workflowDescription, workflowInfo.get("description"));
  }
}
