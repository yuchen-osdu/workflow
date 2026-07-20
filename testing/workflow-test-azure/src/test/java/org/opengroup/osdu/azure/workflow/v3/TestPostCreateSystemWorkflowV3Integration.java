package org.opengroup.osdu.azure.workflow.v3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;
import org.opengroup.osdu.workflow.workflow.v3.PostCreateSystemWorkflowV3IntegrationTests;

import java.util.ArrayList;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

public class TestPostCreateSystemWorkflowV3Integration extends PostCreateSystemWorkflowV3IntegrationTests {
  @BeforeEach
  @Override
  public void setup() {
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeaderWithoutPartition();
    try {
      deleteTestSystemWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterEach
  @Override
  public void tearDown() {
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
    this.createdWorkflows = new ArrayList<>();
  }

  private void deleteAllTestWorkflowRecords() {
    createdWorkflows.stream().forEach(c -> {
      try {
        deleteTestSystemWorkflows(c.get(WORKFLOW_NAME_FIELD));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
