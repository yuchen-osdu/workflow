package org.opengroup.osdu.azure.workflow.framework.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.MAX_SIZE_ALLOWED;

public class TriggerWorkflowTestsBuilder {

  //Trigger Workflow Constants
  public static final String NEW_WORKFLOW_RUN_ID_FIELD = "runId";
  public static final String NEW_WORKFLOW_RUN_TRIGGER_CONFIG = "{\n" +
      "      \"id\":\"opendes:osdu:csv120\",\n" +
      "       \"dataPartitionId\" :\"opendes\",\n" +
      "       \"kind\": \"opendes:osdudemo:wellbore:1.0.0\"\n" +
      "  }";
  public static final String NEW_WORKFLOW_RUN_ADD_PROP = "{\n" +
      "  }";
  public static final String NEW_WORKFLOW_RUN_STATUS_FIELD = "status" ;
  public static final String NEW_WORKFLOW_RUN_STATUS = "submitted" ;

  public static Map<String, Object> buildTriggerWorkflowPayload() {
    Map<String, Object> payload = new HashMap<>();

    payload.put("runId","Workflow_Run_" + System.currentTimeMillis());

    Map<String, Object> executionContext = new HashMap<>();
    executionContext.put("workflowTriggerConfig",new Gson().fromJson(NEW_WORKFLOW_RUN_TRIGGER_CONFIG, JsonObject.class));
    executionContext.put("additionalProperties",new Gson().fromJson(NEW_WORKFLOW_RUN_ADD_PROP, JsonObject.class));

    payload.put("executionContext", executionContext);

    return payload;
  }

  public static Map<String, Object> buildInvalidTriggerWorkflowRunPayload() {
    Map<String, Object> payload = buildTriggerWorkflowPayload();

    //payload.remove("workflowTriggerConfig");
    payload.put("workflowtriggerconfig",new Gson().fromJson(NEW_WORKFLOW_RUN_TRIGGER_CONFIG, JsonObject.class));

    return payload;
  }

  public static String getWorkflowRunIdFromPayload(Map<String, Object> payload) {
    return (String) payload.get(NEW_WORKFLOW_RUN_ID_FIELD);
  }

  public static Map<String, Object> buildTriggerWorkflowPayloadWithMaxRequestSize() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("runId","Workflow_Run_" + System.currentTimeMillis());
    Map<String, Object> executionContext = new HashMap<>();
    JsonObject workflowTriggerConfig = new Gson().fromJson(NEW_WORKFLOW_RUN_TRIGGER_CONFIG, JsonObject.class);
    JsonObject additionalProperties = new Gson().fromJson(NEW_WORKFLOW_RUN_ADD_PROP, JsonObject.class);
    byte[]  dummyObject = new byte[MAX_SIZE_ALLOWED*1000];
    String newId = new String(dummyObject);
    additionalProperties.addProperty("id",newId);
    executionContext.put("workflowTriggerConfig",workflowTriggerConfig);
    executionContext.put("additionalProperties",additionalProperties);
    payload.put("executionContext", executionContext);
    return payload;
  }

}
