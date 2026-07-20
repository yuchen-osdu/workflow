package org.opengroup.osdu.ibm.workflow.workflow.v3;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.ibm.workflow.util.HTTPClientIBM;
import org.opengroup.osdu.workflow.workflow.GetWorkflowRunLatestTaskInfoTest;

public class TestGetWorkflowRunLatestTaskInfoIntegration extends GetWorkflowRunLatestTaskInfoTest {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new HTTPClientIBM();
    this.headers = client.getCommonHeader();
  }

  @AfterEach
  @Override
  public void tearDown() {
    this.client = null;
    this.headers = null;
  }
}
