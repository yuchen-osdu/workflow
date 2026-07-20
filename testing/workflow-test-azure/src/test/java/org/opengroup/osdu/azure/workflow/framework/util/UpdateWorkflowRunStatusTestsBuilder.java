package org.opengroup.osdu.azure.workflow.framework.util;

import java.util.HashMap;
import java.util.Map;

public class UpdateWorkflowRunStatusTestsBuilder {

  public static Map<String, Object> buildUpdateWorkflowRunRunningStatusRequest() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", "running");
    return payload;
  }

  public static Map<String, Object> buildUpdateWorkflowRunFinishedStatusRequest() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", "finished");
    return payload;
  }

  public static Map<String, Object> buildInvalidUpdateWorkflowRunStatusRequestBody(){
    Map<String, Object> payload = new HashMap<>();
    payload.put("staTus", "running");
    return payload;
  }

  public static Map<String, Object> buildInvalidUpdateWorkflowRunStatusRequestBodyIncorrectValue(){
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", "invalid-status");
    return payload;
  }

}
