/*
 *  Copyright 2020-2026 Google LLC
 *  Copyright 2020-2026 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.util;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.DATA_PARTITION_ID_TENANT;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opengroup.osdu.core.test.client.model.workflow.CreateWorkflowRequest;
import org.opengroup.osdu.core.test.client.model.workflow.CreateWorkflowRunRequest;
import org.opengroup.osdu.core.test.client.model.workflow.UpdateWorkflowRunRequest;
import org.opengroup.osdu.workflow.consts.TestConstants;

public class PayloadBuilder {

	private static final String EXTERNAL_AIRFLOW_SECRET_REGISTRATION_INSTRUCTIONS_KEY = "externalAirflowSecret";
	private static final String DAG_NAME = "dagName";
	private static final String TEST_WORKFLOW_DESCRIPTION = "Test workflow record for integration tests.";

	public static CreateWorkflowRequest buildCreateWorkflowValidPayload() {
		return new CreateWorkflowRequest(
				CREATE_WORKFLOW_WORKFLOW_NAME,
				new HashMap<>(),
				TEST_WORKFLOW_DESCRIPTION);
	}

	/**
	 * Builds an external-airflow create-workflow payload with a UUID-suffixed workflowName per
	 * call to avoid 409 Conflict from stale rows left by prior failed/aborted runs. The DAG
	 * pointer ({@code dagName} in {@code registrationInstructions}) is unchanged, so it still
	 * targets the pre-deployed {@code TEST_DAG_NAME_EXTERNAL_AIRFLOW} DAG. Tests that need to
	 * post the same payload twice (e.g. duplicate-create) MUST capture the result of one call
	 * and reuse it; otherwise each call yields a different workflowName.
	 *
	 * <p>Hyphens are stripped from the UUID so the resulting name fits the service-side
	 * {@code ^[a-zA-Z0-9._-]{1,64}$} validator: {@code WORKFLOW_NAME_EXTERNAL_AIRFLOW} (default
	 * 31 chars) + {@code "-"} + 32-char UUID = 64 chars.
	 */
	public static CreateWorkflowRequest buildCreateWorkflowValidPayloadExternalAirflow() {
		return new CreateWorkflowRequest(
				WORKFLOW_NAME_EXTERNAL_AIRFLOW + "-" + UUID.randomUUID().toString().replace("-", ""),
				Map.of(
						DAG_NAME, TestConstants.TEST_DAG_NAME_EXTERNAL_AIRFLOW,
						EXTERNAL_AIRFLOW_SECRET_REGISTRATION_INSTRUCTIONS_KEY, TestConstants.EXTERNAL_AIRFLOW_SECRET),
				"Test workflow record for integration tests(external Airflow).");
	}

	public static CreateWorkflowRequest buildCreateWorkflowPayloadWithIncorrectDag() {
		return new CreateWorkflowRequest(
				CREATE_WORKFLOW_WORKFLOW_NAME,
				Map.of(DAG_NAME, "incorrectDagName"),
				TEST_WORKFLOW_DESCRIPTION);
	}

	public static CreateWorkflowRequest buildCreateWorkflowPayloadWithIncorrectWorkflowName() {
		return new CreateWorkflowRequest(
				"invalid workflow name",
				new HashMap<>(),
				TEST_WORKFLOW_DESCRIPTION);
	}

	public static CreateWorkflowRequest buildCreateWorkflowPayloadWithNoWorkflowName() {
		return new CreateWorkflowRequest(
				"",
				new HashMap<>(),
				TEST_WORKFLOW_DESCRIPTION);
	}

	public static CreateWorkflowRequest buildCreateWorkflowPayloadWithOnlyWorkflowName() {
		return new CreateWorkflowRequest(CREATE_WORKFLOW_WORKFLOW_NAME, null, null);
	}

	public static CreateWorkflowRunRequest buildCreateWorkflowRunValidPayload() {
		Map<String, Object> executionContext = new HashMap<>();
		Map<String, Object> payload = new HashMap<>();

		executionContext.put("workflowID", null);
		payload.put("authorization", null);
		payload.put("data-partition-id", DATA_PARTITION_ID_TENANT);
		payload.put("appKey", "test");

		executionContext.put("payload", payload);
		return new CreateWorkflowRunRequest(executionContext, null);
	}

	public static UpdateWorkflowRunRequest buildUpdateWorkflowPayload() {
		return new UpdateWorkflowRunRequest("finished");
	}

	public static CreateWorkflowRunRequest buildCreateWorkflowRunValidPayloadWithGivenRunId(String runId) {
		CreateWorkflowRunRequest request = buildCreateWorkflowRunValidPayload();
		return new CreateWorkflowRunRequest(request.executionContext(), runId);
	}

	public static UpdateWorkflowRunRequest buildUpdateWorkflowRunValidPayloadWithGivenStatus(String status) {
		return new UpdateWorkflowRunRequest(status);
	}

	public static UpdateWorkflowRunRequest buildUpdateWorkflowRunInvalidPayloadStatus() {
		return new UpdateWorkflowRunRequest("invalid-status");
	}

	public static Map<String, Object> buildUpdateWorkflowRunInvalidRequestPayload() {
		return Map.of("sTaTus", "running");
	}
}
