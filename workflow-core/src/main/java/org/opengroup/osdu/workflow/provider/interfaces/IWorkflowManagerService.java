package org.opengroup.osdu.workflow.provider.interfaces;

import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;

import java.util.List;

public interface IWorkflowManagerService {
  /**
   * Creates workflow with given request.
   * @param request Request object which has information to create workflow.
   * @return Workflow metadata.
   */
  WorkflowMetadata createWorkflow(final CreateWorkflowRequest request);

  /**
   * Creates system workflow with given request.
   * @param request Request object which has information to create workflow.
   * @return Workflow metadata.
   */
  WorkflowMetadata createSystemWorkflow(final CreateWorkflowRequest request);

  /**
   * Returns workflow metadata based on workflowName
   * @param workflowName Id of the workflow for which metadata should be retrieved.
   * @return Workflow metadata
   */
  WorkflowMetadata getWorkflowByName(final String workflowName);

  /**
   * Deletes workflow based on workflowName
   * @param workflowName Id of the workflow which needs to be deleted.
   */
  void deleteWorkflow(final String workflowName);

  /**
   * Deletes system workflow based on workflowName
   * @param workflowName Id of the workflow which needs to be deleted.
   */
  void deleteSystemWorkflow(final String workflowName);

  /**
   * Get List all the workflows for the tenant.
   * @param prefix Filter workflow names which start with the full prefix specified.
   */
  List<WorkflowMetadata> getAllWorkflowForTenant(String prefix);
}
