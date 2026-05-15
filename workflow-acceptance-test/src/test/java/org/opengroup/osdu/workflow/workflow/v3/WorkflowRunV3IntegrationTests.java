/*
 *  Copyright 2020-2025 Google LLC
 *  Copyright 2020-2025 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.workflow.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.EXTERNAL_AIRFLOW_TESTS_ENABLED;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_QUEUED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_RUNNING;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUCCESS;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayloadWithGivenRunId;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunInvalidPayloadStatus;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunInvalidRequestPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunValidPayloadWithGivenStatus;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.deleteCreatedWorkflows;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.deleteWorkflowAndSendFinishedUpdateRequestToWorkflowRuns;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.waitForCreatedWorkflowRunsToComplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.TestExternalAirflow;
import org.opengroup.osdu.workflow.util.v3.TestBase;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;

public final class WorkflowRunV3IntegrationTests extends TestBase {

	private static final String FINISHED = "finished";

	@BeforeAll
  static void beforeAll() throws Exception {
    HTTPClient httpClient = new HTTPClient();
    Map<String, String> headers = httpClient.getCommonHeader();
    deleteWorkflowAndSendFinishedUpdateRequestToWorkflowRuns(CREATE_WORKFLOW_WORKFLOW_NAME, httpClient, headers);
    if (EXTERNAL_AIRFLOW_TESTS_ENABLED) {
      deleteWorkflowAndSendFinishedUpdateRequestToWorkflowRuns(WORKFLOW_NAME_EXTERNAL_AIRFLOW, httpClient, headers);
    }
  }

	@BeforeEach
	@Override
	public void setup() throws Exception {
		this.client = new HTTPClient();
		this.headers = client.getCommonHeader();
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
    this.headers = client.getCommonHeader();
    waitForCreatedWorkflowRunsToComplete(createdWorkflowRuns, client, headers);
    sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns(createdWorkflowRuns, client, headers);
    deleteCreatedWorkflows(createdWorkflowsWorkflowNames, client, headers);
		this.client = null;
		this.headers = null;
	}

	@Test
	public void triggerWorkflowRun_should_returnSuccessAndCompleteExecutionWithImpersonationFlow() throws Exception {
    createAndTrackWorkflow();

		headers.put("on-behalf-of", "impersonatetestmember@test.com");
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		assertTrue(Objects.nonNull(workflowRunInfo.get("submittedBy")));

		Optional<String> userFromEntitlements = getUserFromEntitlements(headers);
		assertEquals(userFromEntitlements.get(), workflowRunInfo.get("submittedBy"));
	}

	protected Optional<String> getUserFromEntitlements(Map<String, String> headers) throws Exception {
		String entitlementV2URL = System.getProperty("ENTITLEMENT_V2_URL", System.getenv("ENTITLEMENT_V2_URL"));
		ClientResponse response = client.send(HttpMethod.GET, entitlementV2URL + "groups", null, headers,
				client.getAccessToken());

		assertEquals(HttpStatus.OK.value(), response.getStatus());

		String entitlementsResponseBody = response.getEntity(String.class);
		Map<String, String> entitlementsResponseMap = new ObjectMapper().readValue(entitlementsResponseBody,
				HashMap.class);
		if (Objects.nonNull(entitlementsResponseMap) && entitlementsResponseMap.containsKey("memberEmail")) {
			return Optional.ofNullable(entitlementsResponseMap.get("memberEmail"));
		}
		return Optional.empty();
	}

	@Test
	public void shouldReturn200WhenTriggerNewWorkflow() throws Exception {
	}

	@Test
	public void shouldReturn200WhenGetAllRunInstances() throws Exception {
	}

	@Test
	public void shouldReturn400WhenGetDetailsForSpecificWorkflowRunInstance() throws Exception {
	}

	@Test
	public void shouldReturn200WhenUpdateWorkflowRunInstance() throws Exception {
	}

	/**
	 * GET WORKFLOW RUN BY ID INTEGRATION TESTS
	 **/

	@Test
	public void getWorkflowRunById_should_returnSuccess_when_givenValidRequest() throws Exception {
    createAndTrackWorkflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();
    String actualWorkflowName = getLastCreatedWorkflowName();

		ClientResponse response = client.send(HttpMethod.GET, String.format(GET_WORKFLOW_RUN_URL,
				actualWorkflowName, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)), null, headers,
				client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
	}

  @TestExternalAirflow
  void getWorkflowRunById_should_returnSuccess_when_givenValidRequestOnExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

		ClientResponse response = client.send(HttpMethod.GET, String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)), null, headers,
				client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
	}

	@Test
	public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
		ClientResponse response = client.send(HttpMethod.GET,
				String.format(GET_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID), null, headers,
				client.getAccessToken());
		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowRunId() throws Exception {
    createAndTrackWorkflow();

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID), null,
				headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

  @TestExternalAirflow
  public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowRunIdOnExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();

    ClientResponse response = client.send(HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID), null,
        headers, client.getAccessToken());

    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

	@Test
	public void getWorkflowRunById_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    createAndTrackWorkflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.GET, String.format(GET_WORKFLOW_RUN_URL,
				getLastCreatedWorkflowName(), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)), null, headers, null);
		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	@Test
	public void getWorkflowRunById_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    createAndTrackWorkflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		ClientResponse response = client
				.send(HttpMethod.GET,
						String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
								workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
						null, headers, client.getNoDataAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void getWorkflowRunById_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    createAndTrackWorkflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				null, HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
				client.getNoDataAccessToken());

		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	/**
	 * POST TRIGGER WORKFLOW RUN INTEGRATION TESTS
	 **/

	@Test
	public void triggerWorkflowRun_should_returnSuccessAndCompleteExecution_when_givenValidTriggerRequest()
			throws Exception {
    createAndTrackWorkflow();

    createAndTrackWorkflowRun();
  }

  @TestExternalAirflow
  public void triggerWorkflowRun_should_returnSuccessAndCompleteExecution_when_givenValidTriggerRequestOnExternalAirflow()
      throws Exception {
    createAndTrackWorkflowExternalAirflow();
    createAndTrackWorkflowRunExternalAirflow();
  }


	/*
	 * after switch to airflow 2.0 stable API, this has to be marked as ignore or
	 * removed. Airflow 2.x stable API will always throw 409 Conflicts instead of
	 * 400 BAD REQUEST when you pass duplicate run id
	 */
	@Test
	@EnabledIfEnvironmentVariable(named = "OSDU_AIRFLOW_VERSION2", matches = "false")
	public void triggerWorkflowRun_should_returnBadRequest_when_givenDuplicateRunId() throws Exception {
    createAndTrackWorkflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(
				workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

		ClientResponse duplicateRunIdResponse = client.send(HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()), duplicateRunIdPayload, headers,
				client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, duplicateRunIdResponse.getStatus());
	}

	/*
	 * Enable this Integration test for airflow 2.0 stable API by removing
	 * '@Disbaled'
	 */
	@Test
	@Disabled("Until switch to airflow 2.0 stable api")
	@EnabledIfEnvironmentVariable(named = "OSDU_AIRFLOW_VERSION2", matches = "true")
	public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId_with_airflow2_stable_API()
			throws Exception {
    createAndTrackWorkflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(
				workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

		ClientResponse duplicateRunIdResponse = client.send(HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()), duplicateRunIdPayload, headers,
				client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_CONFLICT, duplicateRunIdResponse.getStatus());
	}

  @TestExternalAirflow
  public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunIdOnExternalAirflow()
      throws Exception {
    createAndTrackWorkflowExternalAirflow();

    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    String duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(
        workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

    ClientResponse duplicateRunIdResponse = client.send(HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()), duplicateRunIdPayload, headers,
        client.getAccessToken());

    assertEquals(org.apache.http.HttpStatus.SC_CONFLICT, duplicateRunIdResponse.getStatus());
  }

	@Test
	public void triggerWorkflowRun_should_return_WorkflowNotFound_when_givenInvalidWorkflowName() throws Exception {
		ClientResponse response = client.send(HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME), buildCreateWorkflowRunValidPayload(),
				headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void triggerWorkflowRun_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    createAndTrackWorkflow();

		ClientResponse response = client.send(HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
				buildCreateWorkflowRunValidPayload(), headers, null);
		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	@Test
	public void triggerWorkflowRun_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    createAndTrackWorkflow();

		ClientResponse response = client.send(HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
				buildCreateWorkflowRunValidPayload(), headers, client.getNoDataAccessToken());
		assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void triggerWorkflowRun_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    createAndTrackWorkflow();

		Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

		ClientResponse response = client.send(HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()),
				buildCreateWorkflowRunValidPayload(),
				HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION), null);
		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	/**
	 * GET ALL RUN INSTANCES INTEGRATION TESTS
	 **/

	@Test
	public void getAllRunInstances_should_returnSuccess_when_givenValidRequest() throws Exception {
    createAndTrackWorkflow();

    createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()), null, headers,
				client.getAccessToken());
		assertEquals(HttpStatus.OK.value(), response.getStatus());

		String workflowResponseBody = response.getEntity(String.class);
		List<Object> list = new ObjectMapper().readValue(workflowResponseBody, ArrayList.class);
		assertTrue(!list.isEmpty());
	}

  @TestExternalAirflow
  public void getAllRunInstances_should_returnSuccess_when_givenValidRequestOnExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    createAndTrackWorkflowRunExternalAirflow();

    ClientResponse response = client.send(HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, getLastCreatedWorkflowName()), null, headers,
        client.getAccessToken());
    assertEquals(HttpStatus.OK.value(), response.getStatus());

    String workflowResponseBody = response.getEntity(String.class);
    List<Object> list = new ObjectMapper().readValue(workflowResponseBody, ArrayList.class);
    assertTrue(!list.isEmpty());
  }


	@Test
	public void getAllRunInstances_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
    createAndTrackWorkflow();
    createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME), null, headers, client.getAccessToken());
		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void getAllRunInstances_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    createAndTrackWorkflow();
    createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME), null, headers,
				client.getNoDataAccessToken());
		assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void getAllRunInstances_should_returnForbidden_when_notGivenAccessToken() throws Exception {
    createAndTrackWorkflow();
    createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME), null, headers, null);

		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	@Test
	public void getAllRunInstances_should_returnForbidden_when_givenInvalidPartition() throws Exception {
    createAndTrackWorkflow();
    createAndTrackWorkflowRun();

		Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

		ClientResponse response = client.send(HttpMethod.GET,
				String.format(CREATE_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME), null,
				HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION), client.getAccessToken());

		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	/**
	 * PUT UPDATE WORKFLOW RUN STATUS INTEGRATION TESTS
	 **/

	@Test
	public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusRunning() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();
    String actualWorkflowName = getLastCreatedWorkflowName();

		String workflowRunStatus = WORKFLOW_STATUS_TYPE_RUNNING;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName,
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

		Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class),
				HashMap.class);

		assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
		assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
		assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_RUNNING);

		String obtainedWorkflowRunStatus = getWorkflowRunStatus(actualWorkflowName,
				workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

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

  @TestExternalAirflow
  void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusRunning_onExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_RUNNING;

    ClientResponse response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
            workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class),
        HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_RUNNING);

    String obtainedWorkflowRunStatus = getWorkflowRunStatus(getLastCreatedWorkflowName(),
        workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

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
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();
    String actualWorkflowName = getLastCreatedWorkflowName();

		String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName,
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

		Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class),
				HashMap.class);

		assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
		assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
		assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_FINISHED);

		String obtainedWorkflowRunStatus = getWorkflowRunStatus(actualWorkflowName,
				workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

		assertEquals(workflowRunStatus, obtainedWorkflowRunStatus);
	}

  @TestExternalAirflow
  public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusFinished_onExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

    ClientResponse response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
            workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class),
        HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD));

    String obtainedWorkflowRunStatus = getWorkflowRunStatus(getLastCreatedWorkflowName(),
        workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

    assertEquals(workflowRunStatus, obtainedWorkflowRunStatus);
  }

	@Test
	public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidStatus() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunInvalidPayloadStatus(), headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
	}

  @TestExternalAirflow
  public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidStatus_onExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    ClientResponse response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
            workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunInvalidPayloadStatus(), headers, client.getAccessToken());

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

	@Test
	public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidRequestPayload() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunInvalidRequestPayload(), headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
	}

  @TestExternalAirflow
  public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidRequestPayload_onExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    ClientResponse response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
            workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunInvalidRequestPayload(), headers, client.getAccessToken());

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

	@Test
	public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenCompletedWorkflowRun() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();
    String actualWorkflowName = getLastCreatedWorkflowName();

		String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName,
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

		Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class),
				HashMap.class);

		assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
		assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
		assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_FINISHED);

		response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, actualWorkflowName,
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
	}

  @TestExternalAirflow
  void updateWorkflowRunStatus_should_returnBadRequest_when_GivenCompletedWorkflowRun_onExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

    ClientResponse response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
            workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class),
        HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD));

    response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
            workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

    assertEquals(org.apache.http.HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

	@Test
	public void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String workflowRunStatus = WORKFLOW_STATUS_TYPE_FINISHED;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, INVALID_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowRunId() throws Exception {
    createAndTrackWorkflow();
    createAndTrackWorkflowRun();

		String workflowRunStatus = FINISHED;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

  @TestExternalAirflow
  void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowRunId_onExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    createAndTrackWorkflowRunExternalAirflow();

    String workflowRunStatus = FINISHED;

    ClientResponse response = client.send(HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, client.getAccessToken());

    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

	@Test
	public void updateWorkflowRunStatus_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String workflowRunStatus = FINISHED;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers, null);

		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

	@Test
	public void updateWorkflowRunStatus_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String workflowRunStatus = FINISHED;

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus), headers,
				client.getNoDataAccessToken());

		assertEquals(org.apache.http.HttpStatus.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void updateWorkflowRunStatus_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String workflowRunStatus = FINISHED;
		Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);

		ClientResponse response = client.send(HttpMethod.PUT,
				String.format(GET_WORKFLOW_RUN_URL, getLastCreatedWorkflowName(),
						workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
				buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
				HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION), client.getAccessToken());

		assertTrue(org.apache.http.HttpStatus.SC_FORBIDDEN == response.getStatus()
				|| org.apache.http.HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

}
