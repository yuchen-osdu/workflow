package org.opengroup.osdu.workflow.provider.azure.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IWorkflowTasksSharingRepository;
import org.opengroup.osdu.workflow.provider.azure.model.GetSignedUrlResponse;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkflowTasksSharingServiceImplTest {

  private static final String WORKFLOW_ID = "workflow-id";
  private static final String RUN_ID = "run-id";
  private static final String SIGNED_URL = "signed-url";

  @Mock
  private IWorkflowTasksSharingRepository workflowTasksSharingRepository;

  @Mock
  private IWorkflowRunRepository workflowRunRepository;

  @InjectMocks
  private WorkflowTasksSharingServiceImpl workflowTasksSharingService;

  @Test
  public void test_getSignedUrlSuccess() {
    WorkflowRun workflowRun = mock(WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_ID), eq(RUN_ID))).thenReturn(workflowRun);
    when(workflowTasksSharingRepository.getSignedUrl(eq(WORKFLOW_ID), eq(RUN_ID))).thenReturn(SIGNED_URL);
    when(workflowRun.getStatus()).thenReturn(WorkflowStatusType.RUNNING);
    GetSignedUrlResponse getSignedUrlResponse = workflowTasksSharingService.getSignedUrl(WORKFLOW_ID, RUN_ID);

    assertEquals(getSignedUrlResponse.getUrl(), SIGNED_URL);
  }

  @Test
  public void test_getSignedUrlFailure_workflowStatusIsFinished() {
    WorkflowRun workflowRun = mock(WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_ID), eq(RUN_ID))).thenReturn(workflowRun);
    when(workflowRun.getStatus()).thenReturn(WorkflowStatusType.FINISHED);

    Assertions.assertThrows(BadRequestException.class, () -> {
      workflowTasksSharingService.getSignedUrl(WORKFLOW_ID, RUN_ID);
    });
  }

  @Test
  public void test_getSignedUrlFailure_workflowStatusIsFailed() {
    WorkflowRun workflowRun = mock(WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_ID), eq(RUN_ID))).thenReturn(workflowRun);
    when(workflowRun.getStatus()).thenReturn(WorkflowStatusType.FAILED);

    Assertions.assertThrows(BadRequestException.class, () -> {
      workflowTasksSharingService.getSignedUrl(WORKFLOW_ID, RUN_ID);
    });
  }
}
