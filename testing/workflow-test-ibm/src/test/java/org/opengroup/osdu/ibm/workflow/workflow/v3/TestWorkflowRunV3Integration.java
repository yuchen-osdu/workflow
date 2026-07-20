
package org.opengroup.osdu.ibm.workflow.workflow.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_DETAILS_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayloadWithGivenRunId;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.HttpMethod;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.ibm.workflow.util.HTTPClientIBM;
import org.opengroup.osdu.workflow.workflow.v3.WorkflowRunV3IntegrationTests;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_QUEUED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_RUNNING;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUCCESS;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayloadWithGivenRunId;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunValidPayloadWithGivenStatus;

public class TestWorkflowRunV3Integration extends WorkflowRunV3IntegrationTests {

	@BeforeEach
	@Override
	public void setup() throws Exception {
		this.client = new HTTPClientIBM();
		this.headers = client.getCommonHeader();
		try {
			deleteTestWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
		} catch (Exception e) {
			throw e;
		}
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		waitForWorkflowRunsToComplete();
		deleteAllTestWorkflowRecords();
		this.client = null;
		this.headers = null;
	}


	@Override
	@Test
	@Disabled
	public void shouldReturn400WhenGetDetailsForSpecificWorkflowRunInstance() throws Exception {
		String workflowId = UUID
				.randomUUID().toString().replace("-", "");
		String runId = UUID
				.randomUUID().toString().replace("-", "");

		ClientResponse getResponse = client.send(
				HttpMethod.GET,
				String.format(GET_DETAILS_WORKFLOW_RUN_URL, workflowId, runId),
				null,
				headers,
				client.getAccessToken()
				);
		assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatus());
	}


	/*
	 * this test case will not work for airflow 2.0, enable test case :
	 * triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId_with_airflow2_stable_API
	 */
	@Override
	@Test
	@Disabled
	public void triggerWorkflowRun_should_returnBadRequest_when_givenDuplicateRunId() throws Exception {
		//super.triggerWorkflowRun_should_returnBadRequest_when_givenDuplicateRunId();
	}

	@Override
	@Test
	public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId_with_airflow2_stable_API()
			throws Exception {
		String workflowResponseBody = createWorkflow();
		Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
		createdWorkflows.add(workflowInfo);

		String workflowRunResponseBody = createWorkflowRun();
		Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
		createdWorkflowRuns.add(workflowRunInfo);

		String duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));

		ClientResponse duplicateRunIdResponse = client.send(
				HttpMethod.POST,
				String.format(CREATE_WORKFLOW_RUN_URL, CREATE_WORKFLOW_WORKFLOW_NAME),
				duplicateRunIdPayload,
				headers,
				client.getAccessToken()
				);

		assertEquals(org.apache.http.HttpStatus.SC_CONFLICT, duplicateRunIdResponse.getStatus());
	}
  @Override
  @Test
  public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusRunning() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo = new ObjectMapper().readValue(workflowResponseBody, HashMap.class);
    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo = new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowRunInfo);

    String workflowRunStatus = WORKFLOW_STATUS_TYPE_RUNNING;

    ClientResponse response = client.send(
        HttpMethod.PUT,
        String.format(GET_WORKFLOW_RUN_URL, CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD)),
        buildUpdateWorkflowRunValidPayloadWithGivenStatus(workflowRunStatus),
        headers,
        client.getAccessToken()
    );

    Map<String, String> updateWorkflowRunInfo = new ObjectMapper().readValue(response.getEntity(String.class), HashMap.class);

    assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus(), response.toString());
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_ID_FIELD), workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertEquals(updateWorkflowRunInfo.get(WORKFLOW_RUN_STATUS_FIELD), WORKFLOW_STATUS_TYPE_RUNNING);

    String obtainedWorkflowRunStatus = getWorkflowRunStatus(CREATE_WORKFLOW_WORKFLOW_NAME, workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD));
    assertTrue(WORKFLOW_STATUS_TYPE_RUNNING.equals(obtainedWorkflowRunStatus) ||
        WORKFLOW_STATUS_TYPE_SUCCESS.equals(obtainedWorkflowRunStatus) || WORKFLOW_STATUS_TYPE_QUEUED.equals(obtainedWorkflowRunStatus));
  }

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
