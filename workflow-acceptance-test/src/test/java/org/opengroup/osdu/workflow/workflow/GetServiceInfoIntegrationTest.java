/*
 *  Copyright 2021-2025 Google LLC
 *  Copyright 2021-2025 EPAM Systems, Inc
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.EXTERNAL_AIRFLOW_TESTS_ENABLED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.deleteCreatedWorkflows;
import static org.opengroup.osdu.workflow.util.WorkflowApiHelper.deleteWorkflowAndSendFinishedUpdateRequestToWorkflowRuns;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.consts.TestConstants;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.v3.TestBase;
import org.opengroup.osdu.workflow.util.TestExternalAirflow;
import org.opengroup.osdu.workflow.util.VersionInfoUtils;

import com.sun.jersey.api.client.ClientResponse;

import java.util.Map;

public final class GetServiceInfoIntegrationTest extends TestBase {

	protected static final VersionInfoUtils VERSION_INFO_UTILS = new VersionInfoUtils();

  @BeforeAll
  static void beforeAll() throws Exception {
    HTTPClient httpClient = new HTTPClient();
    Map<String, String> headers = httpClient.getCommonHeader();
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
    deleteCreatedWorkflows(createdWorkflowsWorkflowNames, client, headers);
		this.client = null;
		this.headers = null;
	}

	@Test
	public void should_returnInfo() {
		ClientResponse response = callInfoApi(TestConstants.GET_SERVICE_INFO_URL);

		VersionInfoUtils.VersionInfo responseObject = VERSION_INFO_UTILS.getVersionInfoFromResponse(response);

		validateInfoObjectProperties(responseObject);
	}

	@Test
	public void should_returnInfo_withTrailingSlash() {
		ClientResponse response = callInfoApi(TestConstants.GET_SERVICE_INFO_URL + "/");

		VersionInfoUtils.VersionInfo responseObject = VERSION_INFO_UTILS.getVersionInfoFromResponse(response);

		validateInfoObjectProperties(responseObject);
	}

  @TestExternalAirflow
  void should_returnExternalAirflowVersion_when_gettingServiceInfoAfterExternalAirflowInteraction() throws Exception {
    createAndTrackWorkflowExternalAirflow();

    ClientResponse response = callInfoApi(TestConstants.GET_SERVICE_INFO_URL);
    VersionInfoUtils.VersionInfo versionInfo = VERSION_INFO_UTILS.getVersionInfoFromResponse(response);

    assertNotNull(versionInfo.connectedOuterServices);
    assertTrue(versionInfo.connectedOuterServices.size() >= 2);
  }

	private void validateInfoObjectProperties(VersionInfoUtils.VersionInfo versionInfo) {
		assertNotNull(versionInfo.groupId);
		assertNotEquals("", versionInfo.groupId);

		assertNotNull(versionInfo.artifactId);
		assertNotEquals("", versionInfo.artifactId);

		assertNotNull(versionInfo.version);
		assertNotEquals("", versionInfo.version);

		assertNotNull(versionInfo.buildTime);
		assertNotEquals("", versionInfo.buildTime);

		assertNotNull(versionInfo.branch);
		assertNotEquals("", versionInfo.branch);

		assertNotNull(versionInfo.commitId);
		assertNotEquals("", versionInfo.commitId);

		assertNotNull(versionInfo.commitMessage);
		assertNotEquals("", versionInfo.commitMessage);
	}

	private ClientResponse callInfoApi(String url) {
		ClientResponse response = client.send(HttpMethod.GET, url, null, headers, "");

		assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
		return response;
	}
}
