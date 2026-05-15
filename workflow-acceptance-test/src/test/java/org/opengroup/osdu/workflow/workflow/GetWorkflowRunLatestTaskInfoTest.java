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

package org.opengroup.osdu.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.WORKFLOW_HOST;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.EXTERNAL_AIRFLOW_TESTS_ENABLED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.deleteCreatedWorkflows;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.deleteWorkflowAndSendFinishedUpdateRequestToWorkflowRuns;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.waitForCreatedWorkflowRunsToComplete;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.TestExternalAirflow;
import org.opengroup.osdu.workflow.util.v3.TestBase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;

public final class GetWorkflowRunLatestTaskInfoTest extends TestBase {

	public static final String GET_LATEST_DETAILS_BY_ID_API_ENDPOINT = getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST)
			+ "v1/workflow/%s/workflowRun/%s/latestInfo";
	public static final String XCOM_FIELD = "xcom";

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
		this.headers = this.client.getCommonHeader();
  }

	@AfterEach
	@Override
	public void tearDown() throws Exception {
    waitForCreatedWorkflowRunsToComplete(createdWorkflowRuns, client, headers);
    sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns(createdWorkflowRuns, client, headers);
    deleteCreatedWorkflows(createdWorkflowsWorkflowNames, client, headers);
		this.client = null;
		this.headers = null;
	}

	@Test
	public void testGetLatestTaskDetailsOfWorkflowRun() throws Exception {
		String latestRunDetailsUrl = getLatestRunDetailsUrl();

		ClientResponse latestDetailsResponse = client.send(HttpMethod.GET, latestRunDetailsUrl, null, headers,
				client.getAccessToken());

		Map<String, String> latestRunDetails = new ObjectMapper()
				.readValue(latestDetailsResponse.getEntity(String.class), HashMap.class);

		assertNotNull(latestRunDetails.get(XCOM_FIELD));
		assertEquals(org.apache.http.HttpStatus.SC_OK, latestDetailsResponse.getStatus(),
				latestDetailsResponse.toString());
	}

  @TestExternalAirflow
  void testGetLatestTaskDetailsOfWorkflowRunOnExternalAirflow() throws Exception {
    String latestRunDetailsUrl = getLatestRunDetailsUrlExternalAirflow();

    ClientResponse latestDetailsResponse = client.send(HttpMethod.GET, latestRunDetailsUrl, null, headers,
        client.getAccessToken());

    Map<String, String> latestRunDetails = new ObjectMapper()
        .readValue(latestDetailsResponse.getEntity(String.class), HashMap.class);

    assertNotNull(latestRunDetails.get(XCOM_FIELD));
    assertEquals(org.apache.http.HttpStatus.SC_OK, latestDetailsResponse.getStatus(),
        latestDetailsResponse.toString());
  }

	@Test
	public void testGetLatestTaskDetailsOfNotExistingWorkflow() throws Exception {

		String notExistingWorkflowUrl = String.format(GET_LATEST_DETAILS_BY_ID_API_ENDPOINT, INVALID_WORKFLOW_NAME,
				INVALID_WORKFLOW_RUN_ID);

		ClientResponse response = client.send(HttpMethod.GET, notExistingWorkflowUrl, null, headers,
				client.getAccessToken());
		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void testGetLatestTaskDetailsOfNotExistingWorkflowRun() throws Exception {
    createAndTrackWorkflow();

		String existingWorkflowNotExistingRunUrl = String.format(GET_LATEST_DETAILS_BY_ID_API_ENDPOINT,
				CREATE_WORKFLOW_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID);

		ClientResponse response = client.send(HttpMethod.GET, existingWorkflowNotExistingRunUrl, null, headers,
				client.getAccessToken());
		assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
	}

  @TestExternalAirflow
  void testGetLatestTaskDetailsOfNotExistingWorkflowRunOnExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();

    String existingWorkflowNotExistingRunUrl = String.format(GET_LATEST_DETAILS_BY_ID_API_ENDPOINT, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID);

    ClientResponse response = client.send(HttpMethod.GET, existingWorkflowNotExistingRunUrl, null, headers,
        client.getAccessToken());
    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
	public void testGetLatestTaskDetailsWithoutAccess() throws Exception {
		String latestRunDetailsUrl = getLatestRunDetailsUrl();

		ClientResponse latestDetailsResponse = client.send(HttpMethod.GET, latestRunDetailsUrl, null, headers,
				client.getNoDataAccessToken());

		assertEquals(401, latestDetailsResponse.getStatus());
	}

	protected String getLatestRunDetailsUrl() throws Exception {
    createAndTrackWorkflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRun();

		String runId = workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD);

		String latestRunDetailsUrl = String.format(GET_LATEST_DETAILS_BY_ID_API_ENDPOINT, CREATE_WORKFLOW_WORKFLOW_NAME,
				runId);

		Thread.sleep(5000);
		return latestRunDetailsUrl;
	}

  private String getLatestRunDetailsUrlExternalAirflow() throws Exception {
    createAndTrackWorkflowExternalAirflow();
    Map<String, String> workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

    String runId = workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD);

    String latestRunDetailsUrl = String.format(GET_LATEST_DETAILS_BY_ID_API_ENDPOINT, getLastCreatedWorkflowName(),
        runId);

    Thread.sleep(5000);
    return latestRunDetailsUrl;
  }

}
