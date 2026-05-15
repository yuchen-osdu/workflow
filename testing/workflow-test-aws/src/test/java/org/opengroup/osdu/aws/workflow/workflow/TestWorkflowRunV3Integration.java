/**
*  Copyright © 2021 Amazon Web Services
*  Copyright 2020 Google LLC
*  Copyright 2020 EPAM Systems, Inc
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

package org.opengroup.osdu.aws.workflow.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.HttpMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.sun.jersey.api.client.ClientResponse;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.aws.workflow.util.AwsPayloadBuilder;
import org.opengroup.osdu.aws.workflow.util.HTTPClientAWS;
import org.opengroup.osdu.workflow.workflow.v3.WorkflowRunV3IntegrationTests;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_DETAILS_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.junit.Assert.assertEquals;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunValidPayloadWithGivenStatus;

public class TestWorkflowRunV3Integration extends WorkflowRunV3IntegrationTests {

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  @BeforeEach
  @Override
  public void setup() {
    this.client = new HTTPClientAWS();
    this.headers = client.getCommonHeader();

    // cleanup any leftover workflows from previous int test runs
    try {
      deleteTestWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    // waitForWorkflowRunsToComplete();
    Thread.sleep(30000);
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
  }

  @Test
  @Override
  public void shouldReturn400WhenGetDetailsForSpecificWorkflowRunInstance() throws Exception {
    String workflowId = UUID
        .randomUUID().toString().replace("-", "");
    String runId = UUID
        .randomUUID().toString().replace("-", "");

    ClientResponse getResponse = client.send(
        HttpMethod.GET,
        String.format(GET_DETAILS_WORKFLOW_RUN_URL, workflowId, runId),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.NOT_FOUND.value(), getResponse.getStatus());
  }

  private void deleteAllTestWorkflowRecords() {
    createdWorkflows.stream().forEach(c -> {
      try {
        deleteTestWorkflows(c.get(WORKFLOW_NAME_FIELD));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * AWS-only override: send a UUID-suffixed workflow name per run to avoid 409
   * Conflict from stale DynamoDB rows left by prior failed runs.
   * Azure/CIMPL/GC/IBM keep the base behavior (static name), which they require
   * for matching a deployed Airflow DAG.
   */
  @Override
  protected String createWorkflow() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        AwsPayloadBuilder.buildCreateWorkflowValidPayloadWithUniqueName(),
        headers,
        client.getAccessToken()
    );
    assertEquals(response.toString(), HttpStatus.OK.value(), response.getStatus());
    return response.getEntity(String.class);
  }

  @Test
  @Override
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

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
  }

  protected void deleteTestWorkflows(String workflowName) throws Exception {

    List<String> runIds = getWorkflowRuns(workflowName);

    for (String runId : runIds) {
      sendWorkflowRunFinishedUpdateRequest(workflowName, runId);
    }

    String url = CREATE_WORKFLOW_URL + "/" + workflowName;
    sendDeleteRequest(url);
  }

  protected ClientResponse sendWorkflowRunFinishedUpdateRequest(String workflowName, String runId) throws Exception {

    return client.send(
        HttpMethod.PUT,
        String.format(GET_DETAILS_WORKFLOW_RUN_URL, workflowName,
            runId),
        buildUpdateWorkflowPayload(),
        headers,
        client.getAccessToken()
    );
  }

  private List<String> getWorkflowRuns(String workflowName) throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, workflowName),
        null,
        headers,
        client.getAccessToken()
    );

    String respBody = response.getEntity(String.class);

    // Handle case where workflow doesn't exist or has no runs
    if (response.getStatus() != 200) {
      return new ArrayList<String>();
    }

    try {
      JsonArray responseDataArr = gson.fromJson(respBody, JsonArray.class);
      ArrayList<String> runIds = new ArrayList<String>();

      for (JsonElement responseData: responseDataArr) {
          runIds.add(responseData.getAsJsonObject().get("runId").getAsString());
      }

      return runIds;
    } catch (JsonParseException | IllegalStateException | NullPointerException e) {
      // Unexpected body shape for a 200 response; log and assume no runs so teardown can proceed
      System.err.println("getWorkflowRuns: unable to parse response body for workflow '"
          + workflowName + "': " + e.getMessage());
      return new ArrayList<String>();
    }
  }
}
