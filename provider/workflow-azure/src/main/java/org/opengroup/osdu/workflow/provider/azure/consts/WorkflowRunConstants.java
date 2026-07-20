package org.opengroup.osdu.workflow.provider.azure.consts;

public class WorkflowRunConstants {
  // Default number of workflow runs to send in the response when no limit is specified for get all run instances
  public static final int DEFAULT_WORKFLOW_RUNS_LIMIT = 50;
  // Maximum number of workflow runs to that can be sent in the response for get all run instances
  public static final int MAX_WORKFLOW_RUNS_LIMIT = 500;
  // As per the api spec, prefix for workflow run id cannot contain the word "backfill".
  public static final String INVALID_WORKFLOW_RUN_PREFIX = "backfill";
}
