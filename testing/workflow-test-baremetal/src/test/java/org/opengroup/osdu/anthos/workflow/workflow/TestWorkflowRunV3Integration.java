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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.opengroup.osdu.anthos.workflow.util.AnthosHTTTPClient;
import org.opengroup.osdu.workflow.workflow.v3.WorkflowRunV3IntegrationTests;
import org.springframework.http.HttpStatus;

import javax.ws.rs.HttpMethod;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.*;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowPayload;

public class TestWorkflowRunV3Integration extends WorkflowRunV3IntegrationTests {

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private final List<String> activeWorkflowStatusTypes = Arrays.asList("submitted", "running");

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new AnthosHTTTPClient();
    this.headers = client.getCommonHeader();

    try {
      deleteTestWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      throw e;
    }
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    waitForWorkflowRunsToComplete();
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
  }

  private void deleteAllTestWorkflowRecords() {
    createdWorkflows.stream().forEach(c -> {
      try {
        deleteTestWorkflows(c.get(WORKFLOW_NAME_FIELD));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  protected void deleteTestWorkflows(String workflowName) throws Exception {

    List<String> runIds = getWorkflowRuns(workflowName);

    for (String runId : runIds) {
      sendWorkflowRunFinishedUpdateRequest(workflowName, runId);
    }

    String url = CREATE_WORKFLOW_URL + "/" + workflowName;
    sendDeleteRequest(url);
  }

  /*
   * this test case will not work for airflow 2.0
   */
  @Override
  @Test
  @EnabledIfEnvironmentVariable(named = "OSDU_AIRFLOW_VERSION2", matches = "false")
  public void triggerWorkflowRun_should_returnBadRequest_when_givenDuplicateRunId()
      throws Exception {
    super.triggerWorkflowRun_should_returnBadRequest_when_givenDuplicateRunId();
  }

  /*
   * this test case will not work for airflow 1.0
   */
  @Override
  @Test
  @EnabledIfEnvironmentVariable(named = "OSDU_AIRFLOW_VERSION2", matches = "true")
  public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId_with_airflow2_stable_API()
      throws Exception {
    super.triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId_with_airflow2_stable_API();
  }

  @Override
  @Test
  public void triggerWorkflowRun_should_returnSuccessAndCompleteExecutionWithImpersonationFlow() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo =
            new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    headers.put("on-behalf-of", "impersonatetestmember@test.com");
    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo =
            new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);

    assertTrue(Objects.nonNull(workflowRunInfo.get("submittedBy")));

    Optional<String> userFromEntitlements = getUserFromEntitlements(headers);
    assertEquals(userFromEntitlements.get(), workflowRunInfo.get("submittedBy"));
    createdWorkflowRuns.add(workflowRunInfo);
  }

  protected ClientResponse sendWorkflowRunFinishedUpdateRequest(String workflowName, String runId)
      throws Exception {
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

    JsonElement responseDataArr = gson.fromJson(respBody, JsonElement.class);
    ArrayList<String> runIds = new ArrayList<>();

    if (responseDataArr instanceof JsonArray) {

      for (JsonElement responseData : (JsonArray) responseDataArr) {
        String workflowStatus = responseData.getAsJsonObject().get("status").getAsString();

        if (activeWorkflowStatusTypes.contains(workflowStatus)) {
          runIds.add(responseData.getAsJsonObject().get("runId").getAsString());
        }
      }
    }

    return runIds;

  }

  protected Optional<String> getUserFromEntitlements(Map<String, String> headers) throws Exception {
    String entitlementV2URL =
            System.getProperty("ENTITLEMENT_V2_URL", System.getenv("ENTITLEMENT_V2_URL"));
    ClientResponse response =
            client.send(
                    HttpMethod.GET, entitlementV2URL + "groups", null, headers, client.getAccessToken());

    assertEquals(HttpStatus.OK.value(), response.getStatus());

    String entitlementsResponseBody = response.getEntity(String.class);
    Map<String, String> entitlementsResponseMap =
            new ObjectMapper().readValue(entitlementsResponseBody, HashMap.class);
    if (Objects.nonNull(entitlementsResponseMap)
            && entitlementsResponseMap.containsKey("memberEmail")) {
      return Optional.ofNullable(entitlementsResponseMap.get("memberEmail"));
    }
    return Optional.empty();
  }
}
