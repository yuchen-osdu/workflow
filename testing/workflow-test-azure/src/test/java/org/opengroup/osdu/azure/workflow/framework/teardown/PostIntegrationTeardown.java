package org.opengroup.osdu.azure.workflow.framework.teardown;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.workflow.framework.consts.TestConstants;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.TestDataUtil;

import javax.ws.rs.HttpMethod;
import java.util.List;

@Slf4j
public class PostIntegrationTeardown {
  private final HTTPClient client;
  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public PostIntegrationTeardown(HTTPClient client) {
    this.client = client;
  }

  public void run() {
    try {
      deleteAllWorkflows();
    } catch (Exception e) {
      throw new RuntimeException("Tear down failed", e);
    }
  }

  private void deleteAllWorkflows() {
    List<String> createdWorkflowIds = TestDataUtil.getAllWorkflowIds();
    for(String workflowId: createdWorkflowIds) {
      try {
        ClientResponse response = client.send(
            HttpMethod.DELETE,
            String.format(TestConstants.GET_WORKFLOW_URL, workflowId),
            null,
            client.getCommonHeader(),
            client.getAccessToken()
        );

        if(response.getStatus() == HttpStatus.SC_NO_CONTENT) {
          log.info("Successfully delete workflow id {}", workflowId);
        } else {
          log.error("Deleting workflow id {} failed. Got status code {}", workflowId,
              response.getStatus());
        }
      } catch (Exception e) {
        log.error(String.format("Deleting workflow id %s failed", workflowId), e);
      }
    }
  }
}
