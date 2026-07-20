package org.opengroup.osdu.azure.workflow.framework.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.models.WorkflowRun;
import org.opengroup.osdu.azure.workflow.framework.util.AzureTestBase;

import javax.ws.rs.HttpMethod;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_RUN_URL;

public abstract class GetAllRunInstancesIntegrationTests extends AzureTestBase {

  private static final String INVALID_PREFIX = "backfill";
  private static final Integer INVALID_LIMIT = 1000;
  public static final Gson gson = new Gson();

  protected Map<String, String> workflowInfo = null;
  protected Map<String, String> workflowRunInfo = null;

  @Test
  public void should_returnBadRequest_when_givenInvalidPrefix() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL + "?prefix=%s", CREATE_WORKFLOW_WORKFLOW_NAME, INVALID_PREFIX),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void should_returnBadRequest_when_givenInvalidLimit() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL + "?limit=%s", CREATE_WORKFLOW_WORKFLOW_NAME, INVALID_LIMIT),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void should_returnSuccess_when_givenValidLimitAndValidPrefix() throws Exception {
    int limit = 1;
    String prefix = workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD).substring(0, 2);

    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL + "?limit=%s&prefix=%s", CREATE_WORKFLOW_WORKFLOW_NAME, limit, prefix),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    Type WorkflowRunsListType = new TypeToken<ArrayList<WorkflowRun>>(){}.getType();
    List<WorkflowRun> workflowRunsList =
        gson.fromJson(response.getEntity(String.class), WorkflowRunsListType);

    assertEquals(limit, workflowRunsList.size());
    for (WorkflowRun workflowRun : workflowRunsList) {
      assertTrue(workflowRun.getRunId().startsWith(prefix));
    }
  }

  @Test
  public void should_returnSuccess_when_givenValidStartDateParam() throws Exception {
    Long startTimeStamp = Long.parseLong(Objects.toString(workflowInfo.get("creationTimestamp")));
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL + "?startDate=%s", CREATE_WORKFLOW_WORKFLOW_NAME, startTimeStamp),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    Type WorkflowRunsListType = new TypeToken<ArrayList<WorkflowRun>>(){}.getType();
    List<WorkflowRun> workflowRunsList =
        gson.fromJson(response.getEntity(String.class), WorkflowRunsListType);

    assertTrue(workflowRunsList.size() > 0);
    for (WorkflowRun workflowRun : workflowRunsList) {
      assertTrue(workflowRun.getStartTimeStamp() >= startTimeStamp);
    }
  }

  @Test
  public void should_returnSuccess_when_givenValidEndDateParam() throws Exception {
    waitForWorkflowRunsToComplete();
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_RUN_URL, CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        null,
        headers,
        client.getAccessToken()
    );
    String completedWorkflowRunResponse = response.getEntity(String.class);
    Map<String, Object> completedWorkflowRunInfo = new ObjectMapper().readValue(completedWorkflowRunResponse, HashMap.class);
    Long endTimestamp = (Long) completedWorkflowRunInfo.get("endTimeStamp");
    response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL + "?endDate=%s", CREATE_WORKFLOW_WORKFLOW_NAME, endTimestamp),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    Type WorkflowRunsListType = new TypeToken<ArrayList<WorkflowRun>>(){}.getType();
    List<WorkflowRun> workflowRunsList =
        gson.fromJson(response.getEntity(String.class), WorkflowRunsListType);

    assertTrue(workflowRunsList.size() > 0);
    for (WorkflowRun workflowRun : workflowRunsList) {
      assertTrue(workflowRun.getEndTimeStamp() <= endTimestamp);
    }
  }

}
