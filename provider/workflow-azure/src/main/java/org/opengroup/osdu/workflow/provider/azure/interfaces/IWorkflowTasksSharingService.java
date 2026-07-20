package org.opengroup.osdu.workflow.provider.azure.interfaces;


import org.opengroup.osdu.workflow.provider.azure.model.GetSignedUrlResponse;

public interface IWorkflowTasksSharingService {

  GetSignedUrlResponse getSignedUrl(String workflowId, String runId);
}
