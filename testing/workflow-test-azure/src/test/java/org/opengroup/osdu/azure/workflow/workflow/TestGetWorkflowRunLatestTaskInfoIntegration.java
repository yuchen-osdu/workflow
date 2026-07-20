package org.opengroup.osdu.azure.workflow.workflow;

import org.opengroup.osdu.workflow.workflow.GetWorkflowRunLatestTaskInfoTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

public class TestGetWorkflowRunLatestTaskInfoIntegration extends GetWorkflowRunLatestTaskInfoTest {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeader();
  }

  @AfterEach
  @Override
  public void tearDown() {
    this.client = null;
    this.headers = null;
  }
}
