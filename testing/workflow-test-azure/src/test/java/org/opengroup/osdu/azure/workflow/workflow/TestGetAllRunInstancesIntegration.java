package org.opengroup.osdu.azure.workflow.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.opengroup.osdu.azure.workflow.framework.workflow.GetAllRunInstancesIntegrationTests;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

import java.util.ArrayList;
import java.util.HashMap;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestGetAllRunInstancesIntegration extends GetAllRunInstancesIntegrationTests {

  @BeforeAll
  @Override
  public void setup() throws Exception {
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeader();
    try {
      deleteTestWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      e.printStackTrace();
    }
    String workflowResponseBody = createWorkflow();
    workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);
  }

  @AfterAll
  @Override
  public void tearDown() throws Exception {
    waitForWorkflowRunsToComplete();
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
    this.createdWorkflows = new ArrayList<>();
    this.createdWorkflowRuns = new ArrayList<>();
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
}
