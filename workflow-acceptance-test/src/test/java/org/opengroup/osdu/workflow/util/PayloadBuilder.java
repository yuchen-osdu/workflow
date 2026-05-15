package org.opengroup.osdu.workflow.util;

import static java.util.Collections.singletonList;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.DEFAULT_DATA_PARTITION_ID_TENANT1;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.DOMAIN;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.LEGAL_TAG;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.OTHER_RELEVANT_DATA_COUNTRIES;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.DATA_PARTITION_ID_TENANT;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.opengroup.osdu.workflow.consts.TestConstants;

public class PayloadBuilder {

  private static final String EXTERNAL_AIRFLOW_SECRET_REGISTRATION_INSTRUCTIONS_KEY = "externalAirflowSecret";
  private static final String DAG_NAME = "dagName";

  public static String buildWorkflowIdPayload(String workflowId) {
		Map<String, Object> payload = new HashMap<>();

		payload.put("WorkflowID", workflowId);

		return new Gson().toJson(payload);
	}

	public static String buildStartWorkflow(Map<String, Object> context, String type) {
		Map<String, Object> payload = new HashMap<>();

		payload.put("WorkflowType", type);
		payload.put("DataType", "opaque");
		payload.put("Context", context);

		return new Gson().toJson(payload);
	}

	public static String buildUpdateStatus(String workflowId, String status) {
		Map<String, Object> payload = new HashMap<>();

		payload.put("WorkflowID", workflowId);
		payload.put("Status", status);

		return new Gson().toJson(payload);
	}

	public static Map<String, Object> buildContext() {
		Map<String, Object> context = new HashMap<>();

		Map<String, Object> legal = new HashMap<>();
		legal.put("legaltags", singletonList(getEnvironmentVariableOrDefaultKey(LEGAL_TAG)));
		legal.put("otherRelevantDataCountries",
				singletonList(getEnvironmentVariableOrDefaultKey(OTHER_RELEVANT_DATA_COUNTRIES)));

		Map<String, Object> acl = new HashMap<>();
		acl.put("viewers", singletonList(getAcl()));

		context.put("legal", legal);
		context.put("acl", acl);

		return context;
	}

	private static String getAcl() {
		return String.format("data.viewers@%s", getAclSuffix());
	}

	private static String getAclSuffix() {
		return String.format("%s.%s", getEnvironmentVariableOrDefaultKey(DEFAULT_DATA_PARTITION_ID_TENANT1),
				getEnvironmentVariableOrDefaultKey(DOMAIN));
	}

	public static String buildCreateWorkflowValidPayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("workflowName", CREATE_WORKFLOW_WORKFLOW_NAME);
		payload.put("registrationInstructions", new HashMap<String, String>());
		payload.put("description", "Test workflow record for integration tests.");

		return new Gson().toJson(payload);
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
  public static String buildCreateWorkflowValidPayloadExternalAirflow() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(
        "workflowName",
        WORKFLOW_NAME_EXTERNAL_AIRFLOW + "-" + UUID.randomUUID().toString().replace("-", ""));
    payload.put(
        "registrationInstructions",
        Map.of(
            DAG_NAME, TestConstants.TEST_DAG_NAME_EXTERNAL_AIRFLOW,
            EXTERNAL_AIRFLOW_SECRET_REGISTRATION_INSTRUCTIONS_KEY,
                TestConstants.EXTERNAL_AIRFLOW_SECRET));
    payload.put("description", "Test workflow record for integration tests(external Airflow).");

    return new Gson().toJson(payload);
  }

	public static String buildCreateWorkflowPayloadWithIncorrectDag() {
		Map<String, Object> payload = new HashMap<>();
		Map<String, String> registrationInstructions = new HashMap<>();
		registrationInstructions.put(DAG_NAME, "incorrectDagName");
		payload.put("workflowName", CREATE_WORKFLOW_WORKFLOW_NAME);
		payload.put("description", "Test workflow record for integration tests.");
		payload.put("registrationInstructions", registrationInstructions);

		return new Gson().toJson(payload);
	}

	public static String buildCreateWorkflowPayloadWithIncorrectWorkflowName() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("workflowName", "-абвгд-");
		payload.put("description", "Test workflow record for integration tests.");

		return new Gson().toJson(payload);
	}

	public static String buildCreateWorkflowPayloadWithNoWorkflowName() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("workflowName", "");
		payload.put("registrationInstructions", new HashMap<String, String>());
		payload.put("description", "Test workflow record for integration tests.");
		return new Gson().toJson(payload);
	}

	public static String buildCreateWorkflowPayloadWithOnlyWorkflowName() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("workflowName", CREATE_WORKFLOW_WORKFLOW_NAME);
		return new Gson().toJson(payload);
	}

	public static String buildCreateWorkflowRunValidPayload() {
		Map<String, Object> requestBody = new HashMap<>();
		Map<String, Object> executionContext = new HashMap<>();
		Map<String, Object> payload = new HashMap<>();

		executionContext.put("workflowID", null);
		payload.put("authorization", null);
		payload.put("data-partition-id", DATA_PARTITION_ID_TENANT);
		payload.put("appKey", "test");

		executionContext.put("payload", payload);
		requestBody.put("executionContext", executionContext);
		return new Gson().toJson(requestBody);
	}

	public static String buildUpdateWorkflowPayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("status", "finished");
		return new Gson().toJson(payload);
	}

	public static String buildCreateWorkflowRunValidPayloadWithGivenRunId(String runId) throws JsonProcessingException {
		String payload = buildCreateWorkflowRunValidPayload();
		Map<String, Object> requestBody = new ObjectMapper().readValue(payload, HashMap.class);
		requestBody.put("runId", runId);
		return new Gson().toJson(requestBody);
	}

	public static String buildUpdateWorkflowRunValidPayloadWithGivenStatus(String status) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("status", status);
		return new Gson().toJson(payload);
	}

	public static String buildUpdateWorkflowRunInvalidPayloadStatus() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("status", "invalid-status");
		return new Gson().toJson(payload);
	}

	public static String buildUpdateWorkflowRunInvalidRequestPayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("sTaTus", "running");
		return new Gson().toJson(payload);
	}
}
