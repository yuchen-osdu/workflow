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
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.workflow.PostStartWorkflowIntegrationTests;
import org.opengroup.osdu.azure.workflow.utils.AzurePayLoadBuilder;
import org.opengroup.osdu.azure.workflow.utils.DummyRecordsHelper;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.PayloadBuilder;

import javax.ws.rs.HttpMethod;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_STATUS_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.START_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.STATUS_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_ID_FIELD;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUBMITTED;

@Ignore
public class TestPostStartWorkflowIntegration extends PostStartWorkflowIntegrationTests {
  protected static final DummyRecordsHelper RECORDS_HELPER = new DummyRecordsHelper();

	@BeforeEach
	@Override
	public void setup() throws Exception {
	  super.setup();
		this.client = new HTTPClientAzure();
		this.headers = client.getCommonHeader();
	}

  @Test
  @Override
  public void should_returnWorkflowId_when_givenValidRequest() throws Exception{
    ClientResponse response = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        AzurePayLoadBuilder.getValidWorkflowPayload(),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    JsonObject workflowResponse = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);

    assertTrue(isNotBlank(workflowResponse.get(WORKFLOW_ID_FIELD).getAsString()));
  }

  @Test
  public void should_returnWorkflowId_when_givenValidRequest_for_datatype_wellLog() throws Exception{
	  JsonObject dataJsonForWellLog = new JsonObject();
    dataJsonForWellLog.addProperty("WorkflowType","osdu");
    dataJsonForWellLog.addProperty("DataType","well_log");
    dataJsonForWellLog.add("Context", AzurePayLoadBuilder.getWellLogCtxObj());
    ClientResponse response = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        dataJsonForWellLog.toString(),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    JsonObject workflowResponse = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);

    assertTrue(isNotBlank(workflowResponse.get(WORKFLOW_ID_FIELD).getAsString()));
  }

  @Test
  @Override
  public void should_returnSubmitted_when_givenNewlyCreatedWorkflowId() throws Exception {
    ClientResponse workflowStartedResponse = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        AzurePayLoadBuilder.getValidWorkflowPayload(),
        headers,
        client.getAccessToken()
    );

    JsonObject workflowResponse = new Gson().fromJson(workflowStartedResponse.getEntity(String.class), JsonObject.class);
    String startedWorkflowId = workflowResponse.get(WORKFLOW_ID_FIELD).getAsString();


    ClientResponse response = client.send(
        HttpMethod.POST,
        GET_STATUS_URL,
        PayloadBuilder.buildWorkflowIdPayload(startedWorkflowId),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    JsonObject responseBody = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);

    assertEquals(WORKFLOW_STATUS_TYPE_SUBMITTED, responseBody.get(STATUS_FIELD).getAsString());
  }

  @Test
  @Override
  public void should_returnUnauthorized_when_notGivenAccessToken() {
    ClientResponse response = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        AzurePayLoadBuilder.getValidWorkflowPayload(),
        headers,
        null
    );

    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        AzurePayLoadBuilder.getValidWorkflowPayload(),
        HTTPClient.overrideHeader(headers, "invalid-partition"),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  public void should_return_400_bad_request_when_dag_Not_Found ()throws Exception
  {
    ClientResponse response = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        AzurePayLoadBuilder.getInValidWorkflowPayload(),
        headers,
        client.getAccessToken()
    );
    assertNotNull(response);
    DummyRecordsHelper.BadRequestMock responseObject = RECORDS_HELPER.getRecordsMockFromBadRequestResponse(response);
    Assert.assertEquals(responseObject.status,"400");
    String resp="Dag for Workflow type - INGEST, Data type - opaqueo and User id";
    assertThat(responseObject.message,containsString(resp));

  }
  @Test
  public void should_return_400_bad_request_when_Context_is_null ()throws Exception
  {
    JsonObject invalidWorkflowType = new JsonObject();
    invalidWorkflowType.addProperty("WorkflowType","ingest");
    invalidWorkflowType.addProperty("DataType","opaque");
    ClientResponse response = client.send(
        HttpMethod.POST,
        START_WORKFLOW_URL,
        invalidWorkflowType.toString(),
        headers,
        client.getAccessToken()
    );
    DummyRecordsHelper.BadRequestMock responseObject = RECORDS_HELPER.getRecordsMockFromBadRequestResponse(response);
    Assert.assertEquals(responseObject.status,"BAD_REQUEST");
    Assert.assertEquals(responseObject.message,"ConstraintViolationException: Invalid StartWorkflowRequest");
    Assert.assertEquals(responseObject.errors[0],"Context: must not be null");
    assertNotNull(response);
    Assert.assertEquals(HttpStatus.SC_BAD_REQUEST,response.getStatus());

  }

	@AfterEach
	@Override
	public void tearDown() throws Exception {
	  super.tearDown();
		this.client = null;
		this.headers = null;
	}

}
