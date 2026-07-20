package org.opengroup.osdu.workflow.provider.interfaces;

import java.util.List;
import java.util.Map;
import org.opengroup.osdu.workflow.model.TriggerWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunResponse;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;

public interface IWorkflowRunService {
  /**
   * Orchestrates triggering of workflow.
   * @param workflowName Workflow to trigger
   * @param request Request object which has information to trigger workflow.
   * @return Information about workflow run.
   */
  WorkflowRunResponse triggerWorkflow(final String workflowName, TriggerWorkflowRequest request);

  /**
   * Returns Information about workflow run. based on workflowId, runId
   * @param workflowName Name of the workflow for which workflowRun should be checked.
   * @param runId Id of the workflowRun for which metadata should be retrieved.
   * @return Information about workflow run.
   */
  WorkflowRunResponse getWorkflowRunByName(final String workflowName, final String runId);

  /**
   * Deletes all workflow runs information for a workflowId
   * @param workflowName Id of the workflow for which workflowRuns need to be deleted.
   */
  void deleteWorkflowRunsByWorkflowName(final String workflowName);

  /**
   * Get all run instances of a workflow.
   * @param workflowName Name of the workflow for which workflowRuns need to be got.
   * @param params Filter params
   */
  List<WorkflowRun> getAllRunInstancesOfWorkflow(final String workflowName, Map<String, Object> params);

  /**
   * Update workflow run for a workflowName and runId
   * @param workflowName Name of the workflow for which workflowRuns need to be update.
   * @param runId Id of the workflowRun for which metadata should be updated.
   * @param status New status.
   */
  WorkflowRunResponse updateWorkflowRunStatus(final String workflowName, final String runId,
      WorkflowStatusType status);
}
