package org.opengroup.osdu.workflow.provider.interfaces;

import org.opengroup.osdu.workflow.model.WorkflowMetadata;

import java.util.List;

public interface IWorkflowSystemMetadataRepository {

  /**
   * Returns workflow metadata based on workflowName
   * @param workflowName Name of the workflow for which metadata should be retrieved.
   * @return Workflow metadata
   */
  WorkflowMetadata getSystemWorkflow(final String workflowName);

  /**
   * Creates workflow metadata record in persistence store.
   * @param workflowMetadata Workflow metadata object to save in persistence store.
   * @return Workflow metadata
   */
  WorkflowMetadata createSystemWorkflow(final WorkflowMetadata workflowMetadata);

  /**
   * Deletes workflow metadata based on workflowName
   * @param workflowName Name of the workflow for which metadata should be deleted.
   */
  void deleteSystemWorkflow(final String workflowName);

  /**
   * Get all system workflows metadata based on prefix
   * @param prefix Name of the system workflow for which metadata should be deleted.
   */
  List<WorkflowMetadata> getAllSystemWorkflow(final String prefix);
}
