package org.opengroup.osdu.workflow.workflow.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_SYSTEM_WORKFLOW_BY_ID_URL;

import java.util.ArrayList;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.v3.TestBase;

import com.sun.jersey.api.client.ClientResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DeleteSystemWorkflowV3IntegrationTests extends TestBase {

	@BeforeEach
	@Override
	public void setup() throws Exception {
		this.client = new HTTPClient();
		this.headers = client.getCommonHeaderWithoutPartition();
		try {
			deleteTestSystemWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@AfterEach
	@Override
	public void tearDown() {
		deleteAllTestWorkflowRecords();
		this.client = null;
		this.headers = null;
		this.createdWorkflowsWorkflowNames = new ArrayList<>();
	}

	private void deleteAllTestWorkflowRecords() {
		createdWorkflowsWorkflowNames.stream().forEach(workflowName -> {
			try {
				deleteTestSystemWorkflows(workflowName);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		});
	}

	@Test
	public void should_delete_when_givenValidWorkflowName() throws Exception {
    createAndTrackSystemWorkflow();

    ClientResponse deleteResponse = client.send(HttpMethod.DELETE,
				String.format(GET_SYSTEM_WORKFLOW_BY_ID_URL, CREATE_WORKFLOW_WORKFLOW_NAME), null, headers,
				client.getAccessToken());
		assertEquals(HttpStatus.SC_NO_CONTENT, deleteResponse.getStatus());
	}

	@Test
	public void should_ReturnNotFound_when_givenInvalidWorkflowName() throws Exception {
    createAndTrackSystemWorkflow();

    ClientResponse deleteResponse = client.send(HttpMethod.DELETE,
				String.format(GET_SYSTEM_WORKFLOW_BY_ID_URL, INVALID_WORKFLOW_NAME), null, headers,
				client.getAccessToken());

		assertEquals(HttpStatus.SC_NOT_FOUND, deleteResponse.getStatus());
	}

	@Test
	public void should_returnForbidden_when_notGivenAccessToken() throws Exception {
    createAndTrackSystemWorkflow();

    ClientResponse response = client.send(HttpMethod.DELETE,
				String.format(GET_SYSTEM_WORKFLOW_BY_ID_URL, CREATE_WORKFLOW_WORKFLOW_NAME), null, headers, null);
		assertTrue(
				HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}

  private void createAndTrackSystemWorkflow() throws Exception {
    String responseBody = createSystemWorkflow();
    trackWorkflow(responseBody);
  }
}
