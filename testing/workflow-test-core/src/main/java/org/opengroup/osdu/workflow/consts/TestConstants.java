package org.opengroup.osdu.workflow.consts;

import java.util.Arrays;
import java.util.List;

import static org.opengroup.osdu.workflow.consts.DefaultVariable.DEFAULT_DATA_PARTITION_ID_TENANT;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.TEST_DAG_NAME;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.WORKFLOW_HOST;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildContext;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildStartWorkflow;

public class TestConstants {

  //Headers consts
  public static final String HEADER_CORRELATION_ID = "correlation-id";
  public static final String HEADER_DATA_PARTITION_ID = "data-partition-id";
  public static final String HEADER_USER = "user";

  //Api endpoints
  public static final String START_WORKFLOW_API_ENDPOINT = "/startWorkflow";
  public static final String GET_STATUS_API_ENDPOINT = "/getStatus";
  public static final String UPDATE_STATUS_API_ENDPOINT = "/updateStatus";

  public static final String CREATE_WORKFLOW_API_ENDPOINT = "v1/workflow";
  public static final String CREATE_WORKFLOW_RUN_API_ENDPOINT = CREATE_WORKFLOW_API_ENDPOINT + "/%s/workflowRun";
  public static final String GET_WORKFLOW_BY_ID_API_ENDPOINT = "v1/workflow/%s";
  public static final String GET_WORKFLOW_RUN_BY_ID_API_ENDPOINT = CREATE_WORKFLOW_API_ENDPOINT + "/%s/workflowRun/%s";
  public static final String CREATE_SYSTEM_WORKFLOW_API_ENDPOINT ="v1/workflow/system";
  public static final String GET_SYSTEM_WORKFLOW_BY_ID_API_ENDPOINT = "v1/workflow/system/%s";

  public static final String GET_SERVICE_INFO_API_ENDPOINT = "v1/info";
  public static final String GET_ALL_WORKFLOW_PREFIX = "?prefix=";
  public static final String WORKFLOW_TYPE_INGEST = "ingest";

  public static final String WORKFLOW_ID_FIELD = "WorkflowID";
  public static final String STATUS_FIELD = "Status";

  public static final String WORKFLOW_STATUS_TYPE_FINISHED = "finished";
  public static final String WORKFLOW_STATUS_TYPE_SUBMITTED = "submitted";
  public static final String WORKFLOW_STATUS_TYPE_QUEUED = "queued";
  public static final String WORKFLOW_STATUS_TYPE_SUCCESS = "success";
  public static final String WORKFLOW_STATUS_TYPE_RUNNING = "running";
  public static final String WORKFLOW_STATUS_TYPE_FAILED = "failed";
  public static final List<String> FINISHED_WORKFLOW_RUN_STATUSES = Arrays.asList(WORKFLOW_STATUS_TYPE_FINISHED, WORKFLOW_STATUS_TYPE_FAILED,
                                                                                  WORKFLOW_STATUS_TYPE_SUCCESS);

  public static final String START_WORKFLOW_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + START_WORKFLOW_API_ENDPOINT;
  public static final String GET_STATUS_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_STATUS_API_ENDPOINT;
  public static final String UPDATE_STATUS_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + UPDATE_STATUS_API_ENDPOINT;
  public static final String CREATE_WORKFLOW_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + CREATE_WORKFLOW_API_ENDPOINT;
  public static final String CREATE_WORKFLOW_RUN_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + CREATE_WORKFLOW_RUN_API_ENDPOINT;
  public static final String CREATE_SYSTEM_WORKFLOW_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + CREATE_SYSTEM_WORKFLOW_API_ENDPOINT;
  public static final String GET_WORKFLOW_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_WORKFLOW_BY_ID_API_ENDPOINT;
  public static final String GET_WORKFLOW_RUN_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_WORKFLOW_RUN_BY_ID_API_ENDPOINT;
  public static final String GET_SYSTEM_WORKFLOW_BY_ID_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_SYSTEM_WORKFLOW_BY_ID_API_ENDPOINT;
  public static final String GET_DETAILS_WORKFLOW_RUN_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_WORKFLOW_RUN_BY_ID_API_ENDPOINT;
  public static final String GET_SERVICE_INFO_URL =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST) + GET_SERVICE_INFO_API_ENDPOINT;
  public static final String NON_EXISTING_WORKFLOW_ID = "non-existing-workflow-id";

  public static final String WORKFLOW_ID_NOT_BLANK_MESSAGE = "WorkflowID: must not be blank";
  public static final String WORKFLOW_TYPE_NOT_NULL_MESSAGE = "WorkflowType: must not be null";
  public static final String WORKFLOW_ALREADY_HAS_STATUS_MESSAGE = "Workflow status for workflow id: %s already has status:%s and can not be updated";
  public static final String WORKFLOW_STATUS_NOT_ALLOWED_MESSAGE = "Status: Not allowed workflow status type: SUBMITTED, Should be one of: [RUNNING, FINISHED, FAILED]";

  public static final String CREATE_WORKFLOW_WORKFLOW_NAME = getEnvironmentVariableOrDefaultKey(TEST_DAG_NAME);
  public static final String DATA_PARTITION_ID_TENANT = getEnvironmentVariableOrDefaultKey(DEFAULT_DATA_PARTITION_ID_TENANT);

  public static String getValidWorkflowPayload(){
    return buildStartWorkflow(buildContext(), WORKFLOW_TYPE_INGEST);
  }

  public static String getInvalidWorkflowPayload(){
    return buildStartWorkflow(null, null);
  }
}
