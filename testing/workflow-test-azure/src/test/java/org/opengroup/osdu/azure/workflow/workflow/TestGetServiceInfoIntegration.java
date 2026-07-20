package org.opengroup.osdu.azure.workflow.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;
import org.opengroup.osdu.workflow.workflow.GetServiceInfoIntegrationTest;

public class TestGetServiceInfoIntegration extends GetServiceInfoIntegrationTest {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new HTTPClientAzure();
    this.headers = client.getCommonHeader();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    this.client = null;
    this.headers = null;
  }
}
