package org.opengroup.osdu.azure.workflow.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.azure.workflow.framework.workflow.DeleteWorkflowIntegrationTests;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

import java.util.ArrayList;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

public class TestDeleteWorkflowIntegration extends DeleteWorkflowIntegrationTests {

  @BeforeEach
  @Override
  public void setup() {
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeader();
    try {
      deleteTestWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterEach
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
