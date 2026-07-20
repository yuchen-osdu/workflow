package org.opengroup.osdu.workflow.provider.azure.interfaces;

public interface IWorkflowTasksSharingRepository {
  String getSignedUrl(String workflowId, String runId);
}
