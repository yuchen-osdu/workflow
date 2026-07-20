package org.opengroup.osdu.workflow.provider.azure.service;

import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;

import org.opengroup.osdu.workflow.provider.azure.interfaces.IWorkflowTasksSharingService;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IWorkflowTasksSharingRepository;
import org.opengroup.osdu.workflow.provider.azure.model.GetSignedUrlResponse;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkflowTasksSharingServiceImpl implements IWorkflowTasksSharingService {

  @Autowired
  private IWorkflowTasksSharingRepository workflowTasksSharingRepository;

  @Autowired
  private IWorkflowRunRepository workflowRunRepository;

  @Override
  public GetSignedUrlResponse getSignedUrl(String workflowId, String runId) {
    final WorkflowRun workflowRun = workflowRunRepository.getWorkflowRun(workflowId, runId);
    if (workflowRun.getStatus() == WorkflowStatusType.FINISHED
        || workflowRun.getStatus() == WorkflowStatusType.FAILED) {
      throw new BadRequestException("The workflow is either finished or failed");
    }
    return new GetSignedUrlResponse(workflowTasksSharingRepository.getSignedUrl(workflowId, runId));
  }
}
