package org.opengroup.osdu.azure.workflow.framework.workflow;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
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
import static org.opengroup.osdu.workflow.consts.TestConstants.NON_EXISTING_WORKFLOW_ID;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ID_NOT_BLANK_MESSAGE;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildWorkflowIdPayload;

public abstract class PostGetStatusIntegrationTests extends TestBase {

	@Test
	public void should_returnFinished_when_givenFinishedWorkflowId() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID)),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

		String responseBody = response.getEntity(String.class);

		assertTrue(StringUtils.contains(responseBody, WORKFLOW_STATUS_TYPE_FINISHED));
	}

	@Test
	public void should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID)),
				headers,
				null
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID)),
				headers,
				client.getNoDataAccessToken()
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(getEnvironmentVariableOrDefaultKey(FINISHED_WORKFLOW_ID)),
				HTTPClient.overrideHeader(headers, "invalid-partition"),
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void should_returnBadRequest_when_givenEmptyWorkflowId() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(null),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		String error = response.getEntity(String.class);

		assertTrue(error.contains(WORKFLOW_ID_NOT_BLANK_MESSAGE));
	}

	@Test
	public void should_returnNotFound_when_givenNonExistingWorkflowId() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				buildWorkflowIdPayload(NON_EXISTING_WORKFLOW_ID),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
	}
}
