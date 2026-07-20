package org.opengroup.osdu.aws.workflow.workflow;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.aws.workflow.util.HTTPClientAWS;
import org.opengroup.osdu.workflow.workflow.GetWorkflowRunLatestTaskInfoTest;

public class TestGetWorkflowRunLatestTaskInfoIntegration extends GetWorkflowRunLatestTaskInfoTest {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new HTTPClientAWS();
    this.headers = client.getCommonHeader();
  }

  @AfterEach
  @Override
  public void tearDown() {
    this.client = null;
    this.headers = null;
  }
}
