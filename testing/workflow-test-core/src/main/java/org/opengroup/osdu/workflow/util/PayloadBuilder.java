package org.opengroup.osdu.workflow.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.DEFAULT_DATA_PARTITION_ID_TENANT1;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.DOMAIN;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.LEGAL_TAG;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.OTHER_RELEVANT_DATA_COUNTRIES;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.DATA_PARTITION_ID_TENANT;

public class PayloadBuilder {

	public static String buildWorkflowIdPayload(String workflowId){
		Map<String, Object> payload = new HashMap<>();

		payload.put("WorkflowID", workflowId);

		return new Gson().toJson(payload);
	}

	public static String buildStartWorkflow(Map<String, Object> context, String type){
		Map<String, Object> payload = new HashMap<>();

		payload.put("WorkflowType", type);
		payload.put("DataType", "opaque");
		payload.put("Context", context);

		return new Gson().toJson(payload);
	}

	public static String buildUpdateStatus(String workflowId, String status){
		Map <String, Object> payload = new HashMap<>();

		payload.put("WorkflowID", workflowId);
		payload.put("Status", status);

		return new Gson().toJson(payload);
	}

	public static Map<String, Object> buildContext(){
		Map<String, Object> context = new HashMap<>();

		Map<String, Object> legal = new HashMap<>();
		legal.put("legaltags", singletonList(getEnvironmentVariableOrDefaultKey(LEGAL_TAG)));
		legal.put("otherRelevantDataCountries", singletonList(getEnvironmentVariableOrDefaultKey(OTHER_RELEVANT_DATA_COUNTRIES)));

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
		return String.format("%s.%s",
				getEnvironmentVariableOrDefaultKey(DEFAULT_DATA_PARTITION_ID_TENANT1),
				getEnvironmentVariableOrDefaultKey(DOMAIN)
		);
	}

	public static String buildCreateWorkflowValidPayload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workflowName", CREATE_WORKFLOW_WORKFLOW_NAME);
    payload.put("registrationInstructions", new HashMap<String, String>());
    payload.put("description", "Test workflow record for integration tests.");

	  return new Gson().toJson(payload);
  }

  public static String buildCreateWorkflowPayloadWithIncorrectDag() {
    Map<String, Object> payload = new HashMap<>();
    Map<String, String> registrationInstructions = new HashMap<>();
    registrationInstructions.put("dagName", "incorrectDagName");
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

  public static String buildUpdateWorkflowRunInvalidPayloadStatus(){
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", "invalid-status");
    return new Gson().toJson(payload);
  }

  public static String buildUpdateWorkflowRunInvalidRequestPayload(){
    Map<String, Object> payload = new HashMap<>();
    payload.put("sTaTus", "running");
    return new Gson().toJson(payload);
  }
}
