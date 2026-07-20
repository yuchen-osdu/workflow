package org.opengroup.osdu.workflow.provider.interfaces;

import java.util.List;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;

public interface IWorkflowMetadataRepository {
  /**
   * Creates workflow metadata record in persistence store.
   * @param workflowMetadata Workflow metadata object to save in persistence store.
   * @return Workflow metadata
   */
  WorkflowMetadata createWorkflow(final WorkflowMetadata workflowMetadata);

  /**
   * Returns workflow metadata based on workflowName
   * @param workflowName Name of the workflow for which metadata should be retrieved.
   * @return Workflow metadata
   */
  WorkflowMetadata getWorkflow(final String workflowName);

  /**
   * Deletes workflow metadata based on workflowName
   * @param workflowName Name of the workflow for which metadata should be deleted.
   */
  void deleteWorkflow(final String workflowName);

  /**
   * Get all workflows metadata based on prefix
   * @param prefix Name of the workflow for which metadata should be deleted.
   */
  List<WorkflowMetadata> getAllWorkflowForTenant(final String prefix);
}
