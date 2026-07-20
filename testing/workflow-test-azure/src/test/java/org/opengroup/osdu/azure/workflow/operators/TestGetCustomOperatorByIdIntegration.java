package org.opengroup.osdu.azure.workflow.operators;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.azure.workflow.framework.operators.GetCustomOperatorByIdIntegrationTests;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

public class TestGetCustomOperatorByIdIntegration extends GetCustomOperatorByIdIntegrationTests {
  @BeforeEach
  @Override
  public void setup() {
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
