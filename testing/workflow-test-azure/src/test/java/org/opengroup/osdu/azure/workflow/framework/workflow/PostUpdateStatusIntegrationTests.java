package org.opengroup.osdu.azure.workflow.framework.workflow;

import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.TestBase;

import javax.ws.rs.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.FINISHED_WORKFLOW_ID;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_STATUS_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.START_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.STATUS_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.UPDATE_STATUS_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ALREADY_HAS_STATUS_MESSAGE;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ID_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ID_NOT_BLANK_MESSAGE;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_NOT_ALLOWED_MESSAGE;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUBMITTED;
import static org.opengroup.osdu.workflow.consts.TestConstants.getValidWorkflowPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateStatus;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildWorkflowIdPayload;

public abstract class PostUpdateStatusIntegrationTests extends TestBase {

	@Test
	public void should_returnSameWorkflowId_when_givenWorkflowId() throws Exception {
		ClientResponse workflowStartedResponse = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				headers,
				client.getAccessToken()
		);

		JsonObject workflowResponse = gson.fromJson(workflowStartedResponse.getEntity(String.class), JsonObject.class);


		String expectedWorkflowId = workflowResponse.get(WORKFLOW_ID_FIELD).getAsString();

		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(expectedWorkflowId, WORKFLOW_STATUS_TYPE_FINISHED),
				headers,
				client.getAccessToken()
		);

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

		JsonObject responseBody = gson.fromJson(response.getEntity(String.class), JsonObject.class);

		assertEquals(expectedWorkflowId, responseBody.get(WORKFLOW_ID_FIELD).getAsString());
		assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, responseBody.get(STATUS_FIELD).getAsString());
	}

	@Test
	public void should_returnBadRequest_when_givenNullWorkflowId() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(null, WORKFLOW_STATUS_TYPE_FINISHED),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
		String error = response.getEntity(String.class);

		assertTrue(error.contains(WORKFLOW_ID_NOT_BLANK_MESSAGE));
	}

	@Test
	public void should_returnBadRequest_when_givenCurrentWorkflowStatus() throws Exception {
		ClientResponse workflowStartedResponse = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
				getValidWorkflowPayload(),
				headers,
				client.getAccessToken()
		);

		JsonObject workflowResponse = gson.fromJson(workflowStartedResponse.getEntity(String.class), JsonObject.class);

		String expectedWorkflowId = workflowResponse.get(WORKFLOW_ID_FIELD).getAsString();

		ClientResponse statusResponse = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(expectedWorkflowId),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_OK, statusResponse.getStatus());

		JsonObject getStatusResponse = gson.fromJson(statusResponse.getEntity(String.class), JsonObject.class);

		assertEquals(WORKFLOW_STATUS_TYPE_SUBMITTED, getStatusResponse.get(STATUS_FIELD).getAsString());

		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(expectedWorkflowId, WORKFLOW_STATUS_TYPE_SUBMITTED),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		String error = response.getEntity(String.class);

		assertTrue(error.contains(WORKFLOW_STATUS_NOT_ALLOWED_MESSAGE));
	}

	@Test
	public void should_returnBadRequest_when_givenFinishedWorkflowId() throws Exception {
		String finishedWorkflowId = getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID);

		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(finishedWorkflowId, WORKFLOW_STATUS_TYPE_FINISHED),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		String error = response.getEntity(String.class);

		assertTrue(error.contains(
				String.format(WORKFLOW_ALREADY_HAS_STATUS_MESSAGE,
						finishedWorkflowId,
						WORKFLOW_STATUS_TYPE_FINISHED.toUpperCase())));
	}

	@Test
	public void should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID), WORKFLOW_STATUS_TYPE_FINISHED),
				headers,
				null
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID), WORKFLOW_STATUS_TYPE_FINISHED),
				headers,
				client.getNoDataAccessToken()
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				buildUpdateStatus(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID), WORKFLOW_STATUS_TYPE_FINISHED),
				HTTPClient.overrideHeader(headers, "invalid-partition"),
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}
}
