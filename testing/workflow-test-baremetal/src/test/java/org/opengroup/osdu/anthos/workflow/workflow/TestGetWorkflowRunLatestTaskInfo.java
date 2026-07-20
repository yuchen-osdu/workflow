/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.anthos.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.anthos.workflow.util.AnthosHTTTPClient;
import org.opengroup.osdu.workflow.workflow.GetWorkflowRunLatestTaskInfoTest;

public class TestGetWorkflowRunLatestTaskInfo extends GetWorkflowRunLatestTaskInfoTest {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new AnthosHTTTPClient();
    this.headers = this.client.getCommonHeader();
    try {
      deleteTestWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      throw e;
    }
  }

  @AfterEach
  @Override
  public void tearDown() {
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
  }

  @Test
  @Override
  public void testGetLatestTaskDetailsOfWorkflowRun() throws Exception {
    String latestRunDetailsUrl = getLatestRunDetailsUrl();

    ClientResponse latestDetailsResponse =
        client.send(HttpMethod.GET, latestRunDetailsUrl, null, headers, client.getAccessToken());

    Map<String, String> latestRunDetails =
        new ObjectMapper().readValue(latestDetailsResponse.getEntity(String.class), HashMap.class);

    assertNotNull(latestRunDetails.get(XCOM_FIELD));
    assertEquals(
        org.apache.http.HttpStatus.SC_OK,
        latestDetailsResponse.getStatus(),
        latestDetailsResponse.toString());
  }

  @Test
  @Override
  public void testGetLatestTaskDetailsOfNotExistingWorkflow() throws Exception {

    String notExistingWorkflowUrl =
        String.format(
            GET_LATEST_DETAILS_BY_ID_API_ENDPOINT, INVALID_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID);

    ClientResponse response =
        client.send(HttpMethod.GET, notExistingWorkflowUrl, null, headers, client.getAccessToken());
    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  @Override
  public void testGetLatestTaskDetailsOfNotExistingWorkflowRun() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo =
        new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String existingWorkflowNotExistingRunUrl =
        String.format(
            GET_LATEST_DETAILS_BY_ID_API_ENDPOINT,
            CREATE_WORKFLOW_WORKFLOW_NAME,
            INVALID_WORKFLOW_RUN_ID);

    ClientResponse response =
        client.send(
            HttpMethod.GET,
            existingWorkflowNotExistingRunUrl,
            null,
            headers,
            client.getAccessToken());
    assertEquals(org.apache.http.HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  @Override
  public void testGetLatestTaskDetailsWithoutAccess() throws Exception {
    String latestRunDetailsUrl = getLatestRunDetailsUrl();

    ClientResponse latestDetailsResponse =
        client.send(
            HttpMethod.GET, latestRunDetailsUrl, null, headers, client.getNoDataAccessToken());

    assertEquals(401, latestDetailsResponse.getStatus());
  }
}
