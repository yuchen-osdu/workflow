package org.opengroup.osdu.workflow.workflow.v3;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.util.v3.TestBase;

import javax.ws.rs.HttpMethod;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_SYSTEM_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithNoWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayload;

public abstract class PostCreateSystemWorkflowV3IntegrationTests extends TestBase {

  @Test
  public void should_returnWorkflowExists_when_givenDuplicateCreateWorkflowRequest() throws Exception{
    String responseBody = createSystemWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    ClientResponse duplicateResponse = client.send(
        HttpMethod.POST,
        CREATE_SYSTEM_WORKFLOW_URL,
        buildCreateWorkflowValidPayload(),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_CONFLICT, duplicateResponse.getStatus());
  }

  @Test
  public void should_returnBadRequest_when_givenInvalidRequestWithNoWorkflowName() throws Exception{
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_SYSTEM_WORKFLOW_URL,
        buildCreateWorkflowPayloadWithNoWorkflowName(),
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void should_returnBadRequest_when_givenInvalidRequestWithIncorrectWorkflowName() throws Exception{
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_SYSTEM_WORKFLOW_URL,
        buildCreateWorkflowPayloadWithIncorrectWorkflowName(),
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void should_returnForbidden_when_notGivenAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_SYSTEM_WORKFLOW_URL,
        buildCreateWorkflowValidPayload(),
        headers,
        null
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }
}
