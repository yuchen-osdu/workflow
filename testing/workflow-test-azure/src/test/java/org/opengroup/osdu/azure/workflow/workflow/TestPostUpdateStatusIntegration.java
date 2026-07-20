// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.azure.workflow.workflow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.workflow.PostUpdateStatusIntegrationTests;
import org.opengroup.osdu.azure.workflow.utils.AzurePayLoadBuilder;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;
import org.opengroup.osdu.workflow.consts.DefaultVariable;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.PayloadBuilder;

import javax.ws.rs.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_STATUS_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.START_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.STATUS_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.UPDATE_STATUS_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ID_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_NOT_ALLOWED_MESSAGE;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUBMITTED;

@Ignore
public  class TestPostUpdateStatusIntegration extends PostUpdateStatusIntegrationTests {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    super.setup();
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeader();
  }

	@Test
  @Override
	public void should_returnSameWorkflowId_when_givenWorkflowId() throws Exception {
		ClientResponse workflowStartedResponse = client.send(
				HttpMethod.POST,
				START_WORKFLOW_URL,
        AzurePayLoadBuilder.getValidWorkflowPayload(),
				headers,
				client.getAccessToken()
		);

		JsonObject workflowResponse = new Gson().fromJson(workflowStartedResponse.getEntity(String.class), JsonObject.class);


		String expectedWorkflowId = workflowResponse.get(WORKFLOW_ID_FIELD).getAsString();

		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				PayloadBuilder.buildUpdateStatus(expectedWorkflowId, WORKFLOW_STATUS_TYPE_FINISHED),
				headers,client.getAccessToken()
    );
        assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    JsonObject responseBody = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);

    assertEquals(expectedWorkflowId, responseBody.get(WORKFLOW_ID_FIELD).getAsString());
    assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, responseBody.get(STATUS_FIELD).getAsString());
  }



  @Test
  @Override
	public void should_returnBadRequest_when_givenCurrentWorkflowStatus() throws Exception {
  ClientResponse workflowStartedResponse = client.send(
      HttpMethod.POST,
      START_WORKFLOW_URL,
      AzurePayLoadBuilder.getValidWorkflowPayload(),
      headers,
      client.getAccessToken()
  );

		JsonObject workflowResponse = new Gson().fromJson(workflowStartedResponse.getEntity(String.class), JsonObject.class);

		String expectedWorkflowId = workflowResponse.get(WORKFLOW_ID_FIELD).getAsString();

		ClientResponse statusResponse = client.send(
				HttpMethod.POST,
				GET_STATUS_URL,
				PayloadBuilder.buildWorkflowIdPayload(expectedWorkflowId),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_OK, statusResponse.getStatus());

		JsonObject getStatusResponse = new Gson().fromJson(statusResponse.getEntity(String.class), JsonObject.class);

		assertEquals(WORKFLOW_STATUS_TYPE_SUBMITTED, getStatusResponse.get(STATUS_FIELD).getAsString());

		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				PayloadBuilder.buildUpdateStatus(expectedWorkflowId, WORKFLOW_STATUS_TYPE_SUBMITTED),
				headers,
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		String error = response.getEntity(String.class);

		assertTrue(error.contains(WORKFLOW_STATUS_NOT_ALLOWED_MESSAGE));
	}

  @Test
  @Override
  public void should_returnBadRequest_when_givenFinishedWorkflowId() throws Exception {
    String finishedWorkflowId = DefaultVariable.getEnvironmentVariableOrDefaultKey(DefaultVariable.FINISHED_WORKFLOW_ID);

    ClientResponse response = client.send(
        HttpMethod.POST,
        UPDATE_STATUS_URL,
        PayloadBuilder.buildUpdateStatus(finishedWorkflowId, WORKFLOW_STATUS_TYPE_FINISHED),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
  }


	@Test
  @Override
	public void should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				PayloadBuilder.buildUpdateStatus(DefaultVariable.getEnvironmentVariableOrDefaultKey(DefaultVariable.FINISHED_WORKFLOW_ID), WORKFLOW_STATUS_TYPE_FINISHED),
				headers,
				null
		);

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
	}


	@Test
  @Override
	public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
		ClientResponse response = client.send(
				HttpMethod.POST,
				UPDATE_STATUS_URL,
				PayloadBuilder.buildUpdateStatus(DefaultVariable.getEnvironmentVariableOrDefaultKey(DefaultVariable.FINISHED_WORKFLOW_ID), WORKFLOW_STATUS_TYPE_FINISHED),
				HTTPClient.overrideHeader(headers, "invalid-partition"),
				client.getAccessToken()
		);

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
	}

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    this.client = null;
    this.headers = null;
  }
}
