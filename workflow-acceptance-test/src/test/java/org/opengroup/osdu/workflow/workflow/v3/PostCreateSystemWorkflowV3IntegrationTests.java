package org.opengroup.osdu.workflow.workflow.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_SYSTEM_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithNoWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithOnlyWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayload;

import java.util.ArrayList;
import java.util.Map;

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
public final class PostCreateSystemWorkflowV3IntegrationTests extends TestBase {

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
	public void should_returnWorkflowExists_when_givenDuplicateCreateWorkflowRequest() throws Exception {
		String responseBody = createSystemWorkflow();
		Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
		createdWorkflowsWorkflowNames.add(workflowInfo.get(WORKFLOW_NAME_FIELD));

		ClientResponse duplicateResponse = client.send(HttpMethod.POST, CREATE_SYSTEM_WORKFLOW_URL,
				buildCreateWorkflowValidPayload(), headers, client.getAccessToken());

		assertEquals(HttpStatus.SC_CONFLICT, duplicateResponse.getStatus());
	}

	@Test
	public void should_returnBadRequest_when_givenInvalidRequestWithNoWorkflowName() throws Exception {
		ClientResponse response = client.send(HttpMethod.POST, CREATE_SYSTEM_WORKFLOW_URL,
				buildCreateWorkflowPayloadWithNoWorkflowName(), headers, client.getAccessToken());
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void should_returnBadRequest_when_givenInvalidRequestWithOnlyWorkflowName() throws Exception {
		ClientResponse response = client.send(HttpMethod.POST, CREATE_SYSTEM_WORKFLOW_URL,
				buildCreateWorkflowPayloadWithOnlyWorkflowName(), headers, client.getAccessToken());
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void should_returnBadRequest_when_givenInvalidRequestWithIncorrectWorkflowName() throws Exception {
		ClientResponse response = client.send(HttpMethod.POST, CREATE_SYSTEM_WORKFLOW_URL,
				buildCreateWorkflowPayloadWithIncorrectWorkflowName(), headers, client.getAccessToken());
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void should_returnForbidden_when_notGivenAccessToken() throws Exception {
		ClientResponse response = client.send(HttpMethod.POST, CREATE_SYSTEM_WORKFLOW_URL,
				buildCreateWorkflowValidPayload(), headers, null);
		assertTrue(
				HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
	}
}
