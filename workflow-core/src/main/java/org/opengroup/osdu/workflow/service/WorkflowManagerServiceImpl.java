package org.opengroup.osdu.workflow.service;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.logging.AuditLogger;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowSystemMetadataRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowManagerServiceImpl implements IWorkflowManagerService {

  private static final long START_VERSION = 1;

  private final DpsHeaders dpsHeaders;

  private final IWorkflowMetadataRepository workflowMetadataRepository;

  private final IWorkflowSystemMetadataRepository workflowSystemMetadataRepository;

  private final IAirflowResolver airflowResolver;

  private final IWorkflowRunService workflowRunService;

  private final AuditLogger auditLogger;

  @Override
  public WorkflowMetadata createWorkflow(final CreateWorkflowRequest request) {
    return createWorkflowCommon(request, false);
  }

  @Override
  public WorkflowMetadata createSystemWorkflow(final CreateWorkflowRequest request) {
    return createWorkflowCommon(request, true);
  }

  // The same logic is also used in WorkflowRunServiceImpl
  // hence WorkflowRunServiceImpl needs to be changed as well in case the below logic changes
  @Override
  public WorkflowMetadata getWorkflowByName(final String workflowName) {
    try {
      return workflowMetadataRepository.getWorkflow(workflowName);
    } catch (WorkflowNotFoundException e) {
      return workflowSystemMetadataRepository.getSystemWorkflow(workflowName);
    }
  }

  @Override
  public void deleteWorkflow(String workflowName) {
    deleteWorkflowCommon(workflowName, false);
  }

  @Override
  public void deleteSystemWorkflow(String workflowName) {
    deleteWorkflowCommon(workflowName, true);
  }

  @Override
  public List<WorkflowMetadata> getAllWorkflowForTenant(String prefix) {
    List<WorkflowMetadata> workflowMetadataList = workflowMetadataRepository.getAllWorkflowForTenant(prefix);
    List<WorkflowMetadata> workflowSystemMetadataList = workflowSystemMetadataRepository.getAllSystemWorkflow(prefix);

    workflowMetadataList.addAll(workflowSystemMetadataList);
    return workflowMetadataList;
  }

  private void validateWorkflowName(String workflowName) {
    if ((StringUtils.isEmpty(workflowName)) ||
        (!workflowName.matches("^[a-zA-Z0-9._-]{1,64}$"))) {
      throw new BadRequestException("Invalid workflow name provided. Must match pattern ^[a-zA-Z0-9._-]{1,64}$");
    }
  }

  private WorkflowMetadata getWorkflowMetadata(final CreateWorkflowRequest request,
                                               final String createdBy,
                                               final boolean isSystemWorkflow) {
    return WorkflowMetadata.builder()
        .description(request.getDescription())
        .createdBy(createdBy)
        .creationTimestamp(System.currentTimeMillis())
        .version(WorkflowManagerServiceImpl.START_VERSION)
        .registrationInstructions(request.getRegistrationInstructions())
        .workflowName(request.getWorkflowName())
        .isSystemWorkflow(isSystemWorkflow)
        .build();
  }

  private WorkflowMetadata createWorkflowCommon(final CreateWorkflowRequest request, final boolean isSystemWorkflow) {
    validateWorkflowName(request.getWorkflowName());
    final WorkflowMetadata workflowMetadata = getWorkflowMetadata(request, dpsHeaders.getUserEmail(), isSystemWorkflow);
    WorkflowMetadata savedMetadata;
    if (!isSystemWorkflow) {
      savedMetadata = workflowMetadataRepository.createWorkflow(workflowMetadata);
    } else {
      savedMetadata = workflowSystemMetadataRepository.createSystemWorkflow(workflowMetadata);
    }
    final WorkflowEngineRequest rq =
        WorkflowEngineRequest.builder()
            .workflowName(workflowMetadata.getWorkflowName())
            .isSystemWorkflow(isSystemWorkflow)
            .build();
    airflowResolver.getWorkflowEngineService(workflowMetadata).createWorkflow(rq, request.getRegistrationInstructions());
    auditLogger.workflowCreateEvent(Collections.singletonList(savedMetadata.toString()));
    return savedMetadata;
  }

  private void deleteWorkflowCommon(final String workflowName, final boolean isSystemWorkflow) {
    WorkflowMetadata workflowMetadata;
    if (!isSystemWorkflow) {
      workflowMetadata = workflowMetadataRepository.getWorkflow(workflowName);
      workflowRunService.deleteWorkflowRunsByWorkflowName(workflowName);
    } else {
      workflowMetadata = workflowSystemMetadataRepository.getSystemWorkflow(workflowName);
    }
    WorkflowEngineRequest rq = WorkflowEngineRequest.builder()
        .workflowName(workflowName)
        .isDeployedThroughWorkflowService(workflowMetadata.isDeployedThroughWorkflowService())
        .isSystemWorkflow(isSystemWorkflow)
        .build();
    airflowResolver.getWorkflowEngineService(workflowMetadata).deleteWorkflow(rq);
    if (!isSystemWorkflow) {
      workflowMetadataRepository.deleteWorkflow(workflowName);
    } else {
      workflowSystemMetadataRepository.deleteSystemWorkflow(workflowName);
    }
    auditLogger.workflowDeleteEvent(Collections.singletonList(workflowName));
  }
}
