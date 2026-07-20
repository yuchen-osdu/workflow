package org.opengroup.osdu.azure.workflow;

import org.opengroup.osdu.azure.workflow.framework.setup.PreIntegrationSetup;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

public class RunPreIntegrationSetup {
  public static void main(String[] args) {
    HTTPClient client = new HTTPClientAzure();
    PreIntegrationSetup preIntegrationSetup = new PreIntegrationSetup(client);
    preIntegrationSetup.run();
  }
}
