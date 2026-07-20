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

package org.opengroup.osdu.workflow.util;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.FINISHED_WORKFLOW_RUN_STATUSES;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_DETAILS_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowPayload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.sun.jersey.api.client.ClientResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.opengroup.osdu.workflow.util.v3.CreatedWorkflowRun;

@Slf4j
public class WorkflowApiHelper {
  private static final String WORKFLOW_NAME_FIELD = "workflowName";
  private static final String WORKFLOW_RUN_STATUS_FIELD = "status";
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private static final List<String> activeWorkflowStatusTypes =
      Arrays.asList("submitted", "running");

  private WorkflowApiHelper() {}

  public static void deleteWorkflowAndSendFinishedUpdateRequestToWorkflowRuns(
      String workflowName, HTTPClient client, Map<String, String> headers) throws Exception {
    log.info("Deleting test workflow: {}", workflowName);
    List<String> runIds = getWorkflowRuns(workflowName, client, headers);

    for (String runId : runIds) {
      sendWorkflowRunFinishedUpdateRequest(workflowName, runId, client, headers);
    }

    String url = CREATE_WORKFLOW_URL + "/" + workflowName;
    ClientResponse response = sendDeleteRequest(url, client, headers);
    checkClientResponse(response, 204, 404);
  }

  public static void deleteCreatedWorkflows(
      List<String> createdWorkflowsWorkflowNames, HTTPClient client, Map<String, String> headers)
      throws Exception {
    if (createdWorkflowsWorkflowNames == null || createdWorkflowsWorkflowNames.isEmpty()) {
      return;
    }
    log.info("Deleting created workflows: {}", createdWorkflowsWorkflowNames.size());

    for (String workflowName : createdWorkflowsWorkflowNames) {
      String url = CREATE_WORKFLOW_URL + "/" + workflowName;
      ClientResponse response = sendDeleteRequest(url, client, headers);
      checkClientResponse(response, 204, 404);
    }
  }

  private static void checkClientResponse(ClientResponse response, int... allowedCodes)
      throws Exception {
    if (Arrays.stream(allowedCodes).noneMatch(c -> c == response.getStatus())) {
      log.error(
          "Unexpected response status code: {}. Allowed codes: {}. Response: {}",
          response.getStatus(),
          Arrays.toString(allowedCodes),
          response);
      throw new Exception(
          String.format(
              "Unexpected response status code: %s. Allowed codes: %s. Response: %s",
              response.getStatus(), Arrays.toString(allowedCodes), response.getEntity(String.class)));
    }
  }

  public static void sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns(
      List<CreatedWorkflowRun> createdWorkflowRuns,
      HTTPClient client,
      Map<String, String> headers) {
    if (createdWorkflowRuns == null || createdWorkflowRuns.isEmpty()) {
      return;
    }
    log.info(
        "Sending workflow run finished update request for {} runs", createdWorkflowRuns.size());
    for (CreatedWorkflowRun createdWorkflowRun : createdWorkflowRuns) {
      try {
        sendWorkflowRunFinishedUpdateRequest(
            createdWorkflowRun.getWorkflowName(),
            createdWorkflowRun.getWorkflowRunId(),
            client,
            headers);
      } catch (Exception e) {
        log.error(
            "Error finishing workflow run {} for workflow {}: {}",
            createdWorkflowRun.getWorkflowRunId(),
            createdWorkflowRun.getWorkflowName(),
            e.getMessage());
      }
    }
  }

  protected static ClientResponse sendDeleteRequest(
      String url, HTTPClient client, Map<String, String> headers) throws Exception {
    return client.send(HttpMethod.DELETE, url, null, headers, client.getAccessToken());
  }

  protected static ClientResponse sendWorkflowRunFinishedUpdateRequest(
      String workflowName, String runId, HTTPClient client, Map<String, String> headers)
      throws Exception {
    return client.send(
        HttpMethod.PUT,
        String.format(GET_DETAILS_WORKFLOW_RUN_URL, workflowName, runId),
        buildUpdateWorkflowPayload(),
        headers,
        client.getAccessToken());
  }

  private static List<String> getWorkflowRuns(
      String workflowName, HTTPClient client, Map<String, String> headers) throws Exception {
    ClientResponse response =
        client.send(
            HttpMethod.GET,
            String.format(CREATE_WORKFLOW_RUN_URL, workflowName),
            null,
            headers,
            client.getAccessToken());

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

  public static void waitForCreatedWorkflowRunsToComplete(
      List<CreatedWorkflowRun> createdWorkflowRuns,
      HTTPClient client,
      Map<String, String> headers) {

    if (createdWorkflowRuns == null || createdWorkflowRuns.isEmpty()) {
      return;
    }
    log.info("Waiting for {} workflow runs to complete", createdWorkflowRuns.size());

    Set<CreatedWorkflowRun> unfinishedWorkflowRuns = new HashSet<>(createdWorkflowRuns);

    Awaitility.await("")
        .atMost(300, TimeUnit.SECONDS)
        .pollDelay(Duration.ofSeconds(3))
        .pollInterval(Duration.ofSeconds(15))
        .ignoreExceptions()
        .until(
            () -> {
              log.info("Checking {} unfinished workflow runs", unfinishedWorkflowRuns.size());
              Iterator<CreatedWorkflowRun> iterator = unfinishedWorkflowRuns.iterator();
              while (iterator.hasNext()) {
                CreatedWorkflowRun createdWorkflowRun = iterator.next();
                try {
                  String status =
                      getWorkflowRunStatus(
                          createdWorkflowRun.getWorkflowName(),
                          createdWorkflowRun.getWorkflowRunId(),
                          client,
                          headers);
                  if (FINISHED_WORKFLOW_RUN_STATUSES.contains(status)) {
                    iterator.remove();
                  }
                } catch (Exception e) {
                  log.error(
                      "Error while checking workflow run status for {}: {}",
                      createdWorkflowRun.getWorkflowRunId(),
                      e.getMessage());
                }
              }
              return unfinishedWorkflowRuns.isEmpty();
            });
  }

  public static String getWorkflowRunStatus(
      String workflowName, String workflowRunId, HTTPClient client, Map<String, String> headers)
      throws Exception {
    ClientResponse response =
        client.send(
            HttpMethod.GET,
            String.format(GET_WORKFLOW_RUN_URL, workflowName, workflowRunId),
            null,
            headers,
            client.getAccessToken());

    if (response.getStatus() == org.apache.http.HttpStatus.SC_OK) {
      Map<String, String> workflowRunInfo =
          new ObjectMapper().readValue(response.getEntity(String.class), HashMap.class);
      return workflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD);
    } else {
      throw new Exception(
          String.format(
              "Error getting status for workflow run id %s. Status code: %s. Response: %s",
              workflowRunId, response.getStatus(), response));
    }
  }
}
