package org.opengroup.osdu.azure.workflow;

import org.opengroup.osdu.azure.workflow.framework.teardown.PostIntegrationTeardown;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.utils.HTTPClientAzure;

public class RunPostIntegrationTeardown {
  public static void main(String[] args) {
    HTTPClient client = new HTTPClientAzure();
    PostIntegrationTeardown postIntegrationTeardown = new PostIntegrationTeardown(client);
    postIntegrationTeardown.run();
  }
}
