package org.opengroup.osdu.azure.workflow.framework.consts;

import static org.opengroup.osdu.workflow.consts.DefaultVariable.WORKFLOW_HOST;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildContext;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildStartWorkflow;

public class TestConstants extends org.opengroup.osdu.workflow.consts.TestConstants {

  //Api endpoints
  public static final String CREATE_SYSTEM_WORKFLOW_API_ENDPOINT ="v1/workflow/system";
  public static final String GET_SYSTEM_WORKFLOW_BY_ID_API_ENDPOINT = "v1/workflow/system/%s";
  public static final String CUSTOM_OPERATOR_API_ENDPOINT = "v1/customOperator";
  public static final String CUSTOM_OPERATOR_BY_ID_API_ENDPOINT = "v1/customOperator/%s";
  public static final String GET_SIGNED_URL_API_ENDPOINT = "v1/workflow/%s/workflowRun/%s/getSignedUrl";

  public static final String WORKFLOW_TYPE_INGEST = "ingest";

  public static final String WORKFLOW_ID_FIELD = "WorkflowID";
  public static final String STATUS_FIELD = "Status";
  public static final int MAX_SIZE_ALLOWED = 12000;

  public static final String CREATE_SYSTEM_WORKFLOW_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + CREATE_SYSTEM_WORKFLOW_API_ENDPOINT;
  public static final String GET_SYSTEMWORKFLOW_BY_ID_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_SYSTEM_WORKFLOW_BY_ID_API_ENDPOINT;
  public static final String CUSTOM_OPERATOR_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + CUSTOM_OPERATOR_API_ENDPOINT;
  public static final String CUSTOM_OPERATOR_BY_ID_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + CUSTOM_OPERATOR_BY_ID_API_ENDPOINT;
  public static final String GET_SIGNED_URL_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_SIGNED_URL_API_ENDPOINT;

  public static String getValidWorkflowPayload(){
    return buildStartWorkflow(buildContext(), WORKFLOW_TYPE_INGEST);
  }

  public static String getInvalidWorkflowPayload(){
    return buildStartWorkflow(null, null);
  }
}
