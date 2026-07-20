package org.opengroup.osdu.azure.workflow.v3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;
import org.opengroup.osdu.workflow.workflow.v3.WorkflowV3IntegrationTests;

import java.util.ArrayList;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

public class TestWorkflowV3Integration extends WorkflowV3IntegrationTests {

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
  public void tearDown() {
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
    this.createdWorkflows = new ArrayList<>();
  }

  @Override
  @Test
  @Disabled
  public void shouldReturnBadRequestWhenInvalidDagNameWorkflowCreate() throws Exception { }

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
