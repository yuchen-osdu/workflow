package org.opengroup.osdu.azure.workflow.framework.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.workflow.framework.exception.RetryException;
import org.opengroup.osdu.azure.workflow.framework.models.WorkflowRun;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.FINISHED_WORKFLOW_RUN_STATUSES;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.GET_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestDAGNames.TEST_DUMMY_DAG;
import static org.opengroup.osdu.azure.workflow.framework.util.TestDataUtil.getWorkflow;
import static org.opengroup.osdu.azure.workflow.framework.util.TriggerWorkflowTestsBuilder.buildTriggerWorkflowPayload;
import static org.opengroup.osdu.azure.workflow.framework.util.TriggerWorkflowTestsBuilder.getWorkflowRunIdFromPayload;

@Slf4j
public abstract class TestBase {
  public static final Gson gson = new Gson();

  protected HTTPClient client;
  protected Map<String, String> headers;
  protected Map<String, Set<String>> trackedWorkflowRuns;
  protected Set<String> completedWorkflowRunIds;
  private Long integrationTestStartTime;

  public void setup() throws Exception {
    integrationTestStartTime = System.currentTimeMillis();
    log.info("Starting integration test at {}", integrationTestStartTime);
    trackedWorkflowRuns = new HashMap<>();
    completedWorkflowRunIds = new HashSet<>();
  }

  public void tearDown() throws Exception {
    try {
      if(trackedWorkflowRuns.size() > 0) {
        executeWithWaitAndRetry(() -> {
          waitForWorkflowRunsToComplete(trackedWorkflowRuns, completedWorkflowRunIds);
          return null;
        }, 20, 15, TimeUnit.SECONDS);
      }
    } finally {
      Long integrationTestEndTime = System.currentTimeMillis();
      log.info("Completed integration test at {}", integrationTestEndTime);
      log.info("Time taken for completing integration test: {} seconds",
          (integrationTestEndTime - integrationTestStartTime) / 1000);
    }
  }

  protected void waitForWorkflowRunsToComplete(Map<String, Set<String>> workflowIdToRunId,
                                               Set<String> completedWorkflowRunIds) throws Exception {
    for(Map.Entry<String, Set<String>> entry: workflowIdToRunId.entrySet()) {
      String workflowId = entry.getKey();

      for(String workflowRunId: entry.getValue()) {
        if(!completedWorkflowRunIds.contains(workflowRunId)) {
          String workflowRunStatus;
          try {
            workflowRunStatus = getWorkflowRunStatus(workflowId, workflowRunId);
          } catch (Exception e) {
            throw new RetryException(e.getMessage());
          }

          if(FINISHED_WORKFLOW_RUN_STATUSES.contains(workflowRunStatus)) {
            completedWorkflowRunIds.add(workflowRunId);
          } else {
            throw new RetryException(String.format(
                "Unexpected status %s received for workflow run id %s", workflowRunStatus,
                workflowRunId));
          }
        }
      }
    }
  }

  public void trackTriggeredWorkflowRun(String workflowId, String workflowRunId) {
    if(!trackedWorkflowRuns.containsKey(workflowId)) {
      trackedWorkflowRuns.put(workflowId, new HashSet<>());
    }

    trackedWorkflowRuns.get(workflowId).add(workflowRunId);
  }

  public <T> T executeWithWaitAndRetry(Callable<T> callable, int noOfRetries,
                                       long timeToWait, TimeUnit timeUnit) throws Exception {
    long totalTimeToWaitInMillis = timeUnit.toMillis(timeToWait * noOfRetries);
    if(totalTimeToWaitInMillis < 1000) {
      throw new RuntimeException("Minimum wait time should be at least 1 second");
    }
    long waitTimeToAdd = totalTimeToWaitInMillis / noOfRetries;

    long elapsedTime = 0;
    long nextRetryTime = elapsedTime + waitTimeToAdd;
    while(elapsedTime < totalTimeToWaitInMillis) {
      long startTime = System.currentTimeMillis();
      try {
        return callable.call();
      } catch (RetryException e) {
        log.info("Received RetryException: {}. Will wait and retry", e.getMessage());
      } catch (Exception e) {
        log.error("Error calling callable", e);
        throw e;
      }
      long completedTime = System.currentTimeMillis();

      elapsedTime += (completedTime - startTime);
      if(elapsedTime > nextRetryTime) {
        nextRetryTime = (elapsedTime + waitTimeToAdd);
      } else {
        log.info("Waiting for {} seconds", (nextRetryTime - elapsedTime) / 1000);
        Thread.sleep(nextRetryTime - elapsedTime);
        elapsedTime = nextRetryTime;
        nextRetryTime += waitTimeToAdd;
      }
    }
    throw new Exception("Execution failed even after retries");
  }

  public String getWorkflowRunStatus(String workflowId, String workflowRunId) throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, workflowId, workflowRunId),
        null,
        headers,
        client.getAccessToken()
    );

    if(response.getStatus() == HttpStatus.SC_OK) {
      JsonObject responseData = gson.fromJson(response.getEntity(String.class),
          JsonObject.class);
      return responseData.get("status").getAsString();
    } else {
      throw new Exception(String.format("Error getting status for workflow run id %s",
          workflowRunId));
    }
  }

  public WorkflowRun triggerDummyWorkflow(HTTPClient client, Map<String, String> headers)
      throws Exception {
    String workflowId = getWorkflow(TEST_DUMMY_DAG)
        .get(CreateWorkflowTestsBuilder.WORKFLOW_ID_FIELD).getAsString();
    Map<String, Object> triggerWorkflowPayload = buildTriggerWorkflowPayload();

    ClientResponse response = client.send(
        HttpMethod.POST,
        String.format(CREATE_WORKFLOW_RUN_URL, workflowId),
        gson.toJson(triggerWorkflowPayload),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    trackTriggeredWorkflowRun(workflowId, getWorkflowRunIdFromPayload(triggerWorkflowPayload));

    return gson.fromJson(response.getEntity(String.class), WorkflowRun.class);
  }
}
