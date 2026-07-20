package org.opengroup.osdu.azure.workflow.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.opengroup.osdu.azure.workflow.framework.workflow.PostCreateWorkflowIntegrationTests;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

import java.util.ArrayList;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestPostCreateWorkflowIntegration extends PostCreateWorkflowIntegrationTests {

  @BeforeEach
  @Override
  public void setup() {
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeader();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
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
