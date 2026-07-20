package org.opengroup.osdu.azure.workflow.framework.workflow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.AzureTestBase;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.GET_SIGNED_URL_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

public abstract class GetSignedUrlIntegrationTests extends AzureTestBase {

  private static final String INVALID_WORKFLOW_ID = "Invalid-Workflow-ID";
  private static final String INVALID_WORKFLOW_RUN_ID = "Invalid-WorkflowRun-ID";
  private static final String INVALID_PARTITION = "invalid-partition";
  private static final String SIGNED_URL_FIELD = "url";

  protected Map<String, String> workflowInfo = null;
  protected Map<String, String> workflowRunInfo = null;

  @Test
  public void should_returnSuccess_when_givenValidRequest() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_SIGNED_URL_URL, CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    JsonObject workflowResponse = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);
    assertTrue(workflowResponse.has(SIGNED_URL_FIELD));
    String signedUrl = workflowResponse.get(SIGNED_URL_FIELD).getAsString();
    String[] parts = signedUrl.split("[?]");
    assertEquals(parts.length, 2);
  }

  @Test
  public void should_returnNotFound_when_givenInvalidWorkflowId() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_SIGNED_URL_URL, INVALID_WORKFLOW_ID, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void should_returnNotFound_when_givenInvalidWorkflowRunId() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_SIGNED_URL_URL, CREATE_WORKFLOW_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_SIGNED_URL_URL, CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void should_returnForbidden_when_notGivenAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_SIGNED_URL_URL, CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        null
    );
    assertTrue(response.getStatus()== HttpStatus.SC_FORBIDDEN || response.getStatus()== HttpStatus.SC_UNAUTHORIZED) ;
  }

  @Test
  public void should_returnForbidden_when_givenInvalidPartition() throws Exception {
    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_SIGNED_URL_URL, CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }
}
