/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package org.opengroup.osdu.workflow.workflow.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.v3.TestBase;
import org.springframework.http.HttpStatus;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_QUEUED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_RUNNING;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUCCESS;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayloadWithGivenRunId;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunInvalidPayloadStatus;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunInvalidRequestPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunValidPayloadWithGivenStatus;

public abstract class WorkflowRunV3IntegrationTests extends TestBase {
  private static final String INVALID_PREFIX = "backfill";
  private static final Integer INVALID_LIMIT = 1000;


  /** Old methods. Needs to be removed **/

  @Test
  public void shouldReturn200WhenTriggerNewWorkflow() throws Exception { }

  @Test
  public void shouldReturn200WhenGetAllRunInstances() throws Exception { }

  @Test
  public void shouldReturn400WhenGetDetailsForSpecificWorkflowRunInstance() throws Exception { }

  @Test
  public void shouldReturn200WhenUpdateWorkflowRunInstance() throws Exception { }

  /**
   * GET WORKFLOW RUN BY ID INTEGRATION TESTS
   **/

  @Test
  public void getWorkflowRunById_should_returnSuccess_when_givenValidRequest() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);
    String actualWorkflowName = getLastCreatedWorkflowName();

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
  }

  @Test
  public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowRunId() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void getWorkflowRunById_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        null
    );
    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void getWorkflowRunById_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        client.getNoDataAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void getWorkflowRunById_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getNoDataAccessToken()
    );

    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  /**
   * POST TRIGGER WORKFLOW RUN INTEGRATION TESTS
   **/

  @Test
  public void triggerWorkflowRun_should_returnSuccessAndCompleteExecution_when_givenValidTriggerRequest() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);
  }

  /*
   * after switch to airflow 2.0 stable API, this has to be marked as ignore or
   * removed. Airflow 2.x stable API will always throw 409 Conflicts instead of 400
   * BAD REQUEST when you pass duplicate run id
   */
  @Test
  public void triggerWorkflowRun_should_returnBadRequest_when_givenDuplicateRunId() throws  Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

    ClientResponse duplicateRunIdResponse = client.send(
        HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
        duplicateRunIdPayload,
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, duplicateRunIdResponse.getStatus());
  }

  /*
   * Enable this Integration test for airflow 2.0 stable API by removing
   * '@Disbaled'
   */
  @Test
  @Disabled("Until switch to airflow 2.0 stable api")
  public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId_with_airflow2_stable_API() throws  Exception {
	    String workflowResponseBody = createWorkflow();
	    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
	    createdWorkflows.add(workflowInfo);

	    String workflowRunResponseBody = createWorkflowRun();
	    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
	    createdWorkflowRuns.add(workflowRunInfo);

	    String duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

	    ClientResponse duplicateRunIdResponse = client.send(
	        HttpMethod.POST,
	        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
	        duplicateRunIdPayload,
	        headers,
	        client.getAccessToken()
	    );

	    assertEquals(org.apache.http.HttpStatus.SC_CONFLICT, duplicateRunIdResponse.getStatus());
	  }

  @Test
  public void triggerWorkflowRun_should_return_WorkflowNotFound_when_givenInvalidWorkflowName() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME),
        buildCreateWorkflowRunValidPayload(),
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void triggerWorkflowRun_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
        buildCreateWorkflowRunValidPayload(),
        headers,
        null
    );
    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void triggerWorkflowRun_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
        buildCreateWorkflowRunValidPayload(),
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void triggerWorkflowRun_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

    ClientResponse response = client.send(
        HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
        buildCreateWorkflowRunValidPayload(),
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        null
    );
    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  /**
   * GET ALL RUN INSTANCES INTEGRATION TESTS
   **/

  @Test
  public void getAllRunInstances_should_returnSuccess_when_givenValidRequest() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.OK.value(), response.getStatus());

    workflowResponseBody = response.getEntity(String.class);
    List<Object> list =
        new ObjectMapper().readValue(workflowResponseBody, ArrayList.class);
    assertTrue(!list.isEmpty());
  }

  @Test
  public void getAllRunInstances_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void getAllRunInstances_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME),
        null,
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void getAllRunInstances_should_returnForbidden_when_notGivenAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME),
        null,
        headers,
        null
    );

    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void getAllRunInstances_should_returnForbidden_when_givenInvalidPartition() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME),
        null,
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );

    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  /**
   * PUT UPDATE WORKFLOW RUN STATUS INTEGRATION TESTS
   **/

  @Test
  public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusRunning() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);
    String actualWorkflowName = getLastCreatedWorkflowName();

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_RUNNING;

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class), HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_RUNNING);

    String obtainedWorkflowRunStatus = getWorkflowRunStatus(actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

    // Check not only for 'running' but also for 'success' and 'queued' as Airflow may return either
    List<String> expectedStatuses =
        List.of(
            WORKFLOW_STATUS_TYPE_RUNNING,
            WORKFLOW_STATUS_TYPE_SUCCESS,
            WORKFLOW_STATUS_TYPE_QUEUED);
    assertTrue(
        expectedStatuses.contains(obtainedWorkflowRunStatus),
        "Expected status to be one of "
            + expectedStatuses
            + ", but got: "
            + obtainedWorkflowRunStatus);
  }

  @Test
  public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusFinished() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);
    String actualWorkflowName = getLastCreatedWorkflowName();

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class), HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_FINISHED);

    String obtainedWorkflowRunStatus = getWorkflowRunStatus(actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

    assertEquals(workflowRunStatus, obtainedWorkflowRunStatus);
  }

  @Test
  public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidStatus() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunInvalidPayloadStatus(),
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidRequestPayload() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunInvalidRequestPayload(),
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenCompletedWorkflowRun() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);
    String actualWorkflowName = getLastCreatedWorkflowName();

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class), HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_FINISHED);

    response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowRunId() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String workflowRunStatus = "finished";

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String workflowRunStatus = "finished";

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        null
    );

    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String workflowRunStatus = "finished";

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getNoDataAccessToken()
    );

    assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void updateWorkflowRunStatus_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String workflowRunStatus = "finished";
    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );

    assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus() || org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void triggerWorkflowRun_should_returnSuccessAndCompleteExecutionWithImpersonationFlow()
      throws Exception {}
}
