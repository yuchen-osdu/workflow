package org.opengroup.osdu.workflow.workflow;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_STATUS_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.START_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.STATUS_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ID_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUBMITTED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_TYPE_NOT_NULL_MESSAGE;
import static org.opengroup.osdu.workflow.consts.TestConstants.getInvalidWorkflowPayload;
import static org.opengroup.osdu.workflow.consts.TestConstants.getValidWorkflowPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildWorkflowIdPayload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import javax.ws.rs.HttpMethod;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.TestBase;

public abstract class PostStartWorkflowIntegrationTests extends TestBase {

	@Test
	public void should_returnWorkflowId_when_givenValidRequest() throws Exception{
		ClientResponse response = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

		JsonObject workflowResponse = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);

		assertTrue(isNotBlank(workflowResponse.get(WORKFLOW_ID_FIELD).getAsString()));
	}

	@Test
	public void should_returnBadRequest_when_givenInvalidRequest() throws Exception{
		ClientResponse response = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getInvalidWorkflowPayload(),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		String error = response.getEntity(String.class);

		assertTrue(error.contains(WORKFLOW_TYPE_NOT_NULL_MESSAGE));
	}

	@Test
	public void should_returnSubmitted_when_givenNewlyCreatedWorkflowId() throws Exception {
		ClientResponse workflowStartedResponse = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				headers,
				client.getAccessToken()
		);

		JsonObject workflowResponse = new Gson().fromJson(workflowStartedResponse.getEntity(String.class), JsonObject.class);
		String startedWorkflowId = workflowResponse.get(WORKFLOW_ID_FIELD).getAsString();


		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(startedWorkflowId),
				headers,
				client.getAccessToken()
		);

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

		JsonObject responseBody = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);

		assertEquals(WORKFLOW_STATUS_TYPE_SUBMITTED, responseBody.get(STATUS_FIELD).getAsString());
	}

	@Test
	public void should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				headers,
				null
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				headers,
				client.getNoDataAccessToken()
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				HTTPClient.overrideHeader(headers, "invalid-partition"),
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}
}
