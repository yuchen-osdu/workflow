package org.opengroup.osdu.workflow.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.status.Status;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.exception.WorkflowRunCompletedException;
import org.opengroup.osdu.workflow.exception.WorkflowRunNotFoundException;
import org.opengroup.osdu.workflow.gsm.WorkflowStatusPublisher;
import org.opengroup.osdu.workflow.logging.AuditLogger;
import org.opengroup.osdu.workflow.model.TriggerWorkflowRequest;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.UpdateWorkflowRunRequest;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunResponse;
import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowSystemMetadataRepository;
import org.opengroup.osdu.workflow.service.WorkflowRunServiceImpl;

/**
 * Tests for {@link WorkflowRunServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
class WorkflowRunServiceTest {

  private static final String KEY_RUN_ID = "run_id";
  private static final String KEY_AUTH_TOKEN = "authToken";
  private static final String KEY_EXECUTION_CONTEXT = "execution_context";
  private static final String KEY_WORKFLOW_NAME = "workflow_name";
  private static final String KEY_CORRELATION_ID = "correlation_id";
  private static final String KEY_USER_ID = "userId";
  private static final String AUTH_TOKEN = "Bearer Dummy";
  private static final String WORKFLOW_NAME = "some-dag-name";
  private static final String CORRELATION_ID = "some-correlation-id";
  private static final String RUN_ID = "d13f7fd0-d27e-4176-8d60-6e9aad86e347";
  private static final String USER_EMAIL = "user@email.com";
  private static final String TEST_CURSOR = "test-cursor";
  private static final String EXECUTION_DATE = "2021-01-05T11:36:45+00:00";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String USER_ID = "dummy-user-id";
  private static final String WORKFLOW_METADATA = "{\n" +
      "  \"workflowId\": \"some-dag-name\",\n" +
      "  \"workflowName\": \"some-dag-name\",\n" +
      " \"registrationInstructions\": {\n" +
      " \"dagName\": \"dag-name\"\n" +
      "},\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"creationTimestamp\": 1600144876028,\n" +
      "  \"createdBy\": \"user@email.com\",\n" +
      "  \"version\": 1\n" +
      "}";

  private static final String SYSTEM_WORKFLOW_METADATA = "{\n" +
      "  \"workflowId\": \"some-dag-name\",\n" +
      "  \"workflowName\": \"some-dag-name\",\n" +
      "  \"isSystemWorkflow\": true,\n" +
      " \"registrationInstructions\": {\n" +
      " \"dagName\": \"dag-name\"\n" +
      "},\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"creationTimestamp\": 1600144876028,\n" +
      "  \"createdBy\": \"user@email.com\",\n" +
      "  \"version\": 1\n" +
      "}";

  private static final String WORKFLOW_TRIGGER_REQUEST_DATA = "{\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"executionContext\": {\n" +
      "  }\n" +
      "}";
  private static final String SUBMITTED_WORKFLOW_RUN = "{\n" +
      "  \"workflowId\": \"SGVsbG9Xb3JsZA==\",\n" +
      "  \"workflowName\": \"some-dag-name\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"startTimeStamp\": 1236331,\n" +
      "  \"workflowEngineExecutionDate\": \"2021-01-05T11:36:45+00:00\",\n" +
      "  \"status\": \"submitted\",\n" +
      "  \"submittedBy\": \"0b16033e-6e20-481f-9951-ad59efbf88fc\"\n" +
      "}";
  private static final String RUNNING_WORKFLOW_RUN = "{\n" +
      "  \"workflowId\": \"SGVsbG9Xb3JsZA==\",\n" +
      "  \"workflowName\": \"some-dag-name\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e34523\",\n" +
      "  \"startTimeStamp\": 1236331,\n" +
      "  \"workflowEngineExecutionDate\": \"2021-01-05T11:36:45+00:00\",\n" +
      "  \"status\": \"running\",\n" +
      "  \"submittedBy\": \"0b16033e-6e20-481f-9951-ad59efbf88fc\"\n" +
      "}";
  private static final String FINISHED_WORKFLOW_RUN = "{\n" +
      "  \"workflowId\": \"SGVsbG9Xb3JsZA==\",\n" +
      "  \"workflowName\": \"some-dag-name\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86eq781641\",\n" +
      "  \"startTimeStamp\": 1236331,\n" +
      "  \"workflowEngineExecutionDate\": \"2021-01-05T11:36:45+00:00\",\n" +
      "  \"endTimeStamp\": 1231238912,\n" +
      "  \"status\": \"finished\",\n" +
      "  \"submittedBy\": \"0b16033e-6e20-481f-9951-ad59efbf88fc\"\n" +
      "}";
  private static final String WORKFLOW_RUN_UPDATE_RUNNING_STATUS_REQUEST_DATA = "{\n" +
      "  \"status\": \"running\"\n" +
      "}";
  private static final String WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA = "{\n" +
      "  \"status\": \"finished\"\n" +
      "}";

  private static final String WORKFLOW_TRIGGER_RESPONSE = "{\n" +
      "  \"execution_date\": \"2021-01-05T11:36:45+00:00\",\n" +
      "  \"message\": \"Created <DagRun HelloWorld @ 2021-01-05 11:36:45+00:00: d13f7fd0-d27e-4176-8d60-6e9aad86e347, externally triggered: True>\",\n" +
      "  \"run_id\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\"\n" +
      "}";

  @Mock
  private IWorkflowMetadataRepository workflowMetadataRepository;

  @Mock
  private IWorkflowSystemMetadataRepository workflowSystemMetadataRepository;

  @Mock
  private IWorkflowRunRepository workflowRunRepository;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private IWorkflowEngineService workflowEngineService;

  @Mock
  private AuditLogger auditLogger;

  @Mock
  private WorkflowStatusPublisher statusPublisher;

  @Mock
  private IAirflowResolver airflowResolver;

  @InjectMocks
  private WorkflowRunServiceImpl workflowRunService;

  @Test
  void testTriggerWorkflowWithExistingWorkflowId() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER
        .readValue(WORKFLOW_METADATA, WorkflowMetadata.class);
	  final TriggerWorkflowResponse triggerWorkflowResponse = OBJECT_MAPPER
      .readValue(WORKFLOW_TRIGGER_RESPONSE, TriggerWorkflowResponse.class);
    final TriggerWorkflowRequest request =
        OBJECT_MAPPER.readValue(WORKFLOW_TRIGGER_REQUEST_DATA, TriggerWorkflowRequest.class);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
	  when(workflowEngineService.triggerWorkflow(workflowEngineRequestArgumentCaptor.capture(),
        eq(createWorkflowPayload(RUN_ID, request)))).thenReturn(triggerWorkflowResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(AUTH_TOKEN);
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowRun.class);
    final WorkflowRun responseWorkflowRun = mock(WorkflowRun.class);
    when(workflowRunRepository.saveWorkflowRun(workflowRunArgumentCaptor.capture()))
        .thenReturn(responseWorkflowRun);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when
    final WorkflowRunResponse returnedWorkflowRun = workflowRunService
        .triggerWorkflow(WORKFLOW_NAME, request);

    //then
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowSystemMetadataRepository, times(0)).getSystemWorkflow(any());
    verify(workflowEngineService)
        .triggerWorkflow(any(WorkflowEngineRequest.class), eq(createWorkflowPayload(RUN_ID, request)));
    WorkflowEngineRequest capturedWorkflowEngineRequest =
        workflowEngineRequestArgumentCaptor.getValue();
    assertThat(capturedWorkflowEngineRequest.getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.getRunId(), equalTo(RUN_ID));
    assertThat(capturedWorkflowEngineRequest.getWorkflowId(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.isSystemWorkflow(), equalTo(false));
    verify(workflowRunRepository).saveWorkflowRun(any(WorkflowRun.class));
	  verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getUserEmail();
    verify(dpsHeaders).getCorrelationId();
    verify(statusPublisher).publishStatusWithNoErrors(any(), any(DpsHeaders.class), any(String.class), any(Status.class));
    assertThat(returnedWorkflowRun, equalTo(buildWorkflowRunResponse(responseWorkflowRun)));
    assertThat(workflowRunArgumentCaptor.getValue().getRunId(), equalTo(RUN_ID));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(workflowRunArgumentCaptor.getValue().getSubmittedBy(), equalTo(USER_EMAIL));
	  assertThat(workflowRunArgumentCaptor.getValue().getWorkflowEngineExecutionDate(), equalTo(EXECUTION_DATE));
    assertThat(workflowRunArgumentCaptor.getValue().getStatus(),
        equalTo(WorkflowStatusType.SUBMITTED));
  }

  @Test
  void testTriggerWorkflowWithExistingSystemWorkflowId() throws Exception {
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER
        .readValue(SYSTEM_WORKFLOW_METADATA, WorkflowMetadata.class);
    final TriggerWorkflowResponse triggerWorkflowResponse = OBJECT_MAPPER
        .readValue(WORKFLOW_TRIGGER_RESPONSE, TriggerWorkflowResponse.class);
    final ArgumentCaptor<Long> startTimeStampArgumentCaptor = ArgumentCaptor.forClass(Long.class);
    final TriggerWorkflowRequest request =
        OBJECT_MAPPER.readValue(WORKFLOW_TRIGGER_REQUEST_DATA, TriggerWorkflowRequest.class);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME)))
        .thenThrow(WorkflowNotFoundException.class);
    when(workflowSystemMetadataRepository.getSystemWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    when(workflowEngineService.triggerWorkflow(workflowEngineRequestArgumentCaptor.capture(),
        eq(createWorkflowPayload(RUN_ID, request)))).thenReturn(triggerWorkflowResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(AUTH_TOKEN);
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowRun.class);
    final WorkflowRun responseWorkflowRun = mock(WorkflowRun.class);
    when(workflowRunRepository.saveWorkflowRun(workflowRunArgumentCaptor.capture()))
        .thenReturn(responseWorkflowRun);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when
    final WorkflowRunResponse returnedWorkflowRun = workflowRunService
        .triggerWorkflow(WORKFLOW_NAME, request);

    //then
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowSystemMetadataRepository).getSystemWorkflow(eq(WORKFLOW_NAME));
    verify(workflowEngineService)
        .triggerWorkflow(any(WorkflowEngineRequest.class), eq(createWorkflowPayload(RUN_ID, request)));
    WorkflowEngineRequest capturedWorkflowEngineRequest =
        workflowEngineRequestArgumentCaptor.getValue();
    assertThat(capturedWorkflowEngineRequest.getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.getRunId(), equalTo(RUN_ID));
    assertThat(capturedWorkflowEngineRequest.getWorkflowId(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.isSystemWorkflow(), equalTo(true));
    verify(workflowRunRepository).saveWorkflowRun(any(WorkflowRun.class));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getUserEmail();
    verify(dpsHeaders).getCorrelationId();
    verify(statusPublisher).publishStatusWithNoErrors(any(), any(DpsHeaders.class), any(String.class), any(Status.class));
    assertThat(returnedWorkflowRun, equalTo(buildWorkflowRunResponse(responseWorkflowRun)));
    assertThat(workflowRunArgumentCaptor.getValue().getRunId(), equalTo(RUN_ID));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(workflowRunArgumentCaptor.getValue().getSubmittedBy(), equalTo(USER_EMAIL));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowEngineExecutionDate(), equalTo(EXECUTION_DATE));
    assertThat(workflowRunArgumentCaptor.getValue().getStatus(),
        equalTo(WorkflowStatusType.SUBMITTED));
  }

  @Test
  void testTriggerWorkflowWithNonExistingWorkflowId() throws Exception {
    //given
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME)))
        .thenThrow(WorkflowNotFoundException.class);
    when(workflowSystemMetadataRepository.getSystemWorkflow(eq(WORKFLOW_NAME)))
        .thenThrow(WorkflowNotFoundException.class);
    final TriggerWorkflowRequest request =
        OBJECT_MAPPER.readValue(WORKFLOW_TRIGGER_REQUEST_DATA, TriggerWorkflowRequest.class);

    //when and then
    Assertions.assertThrows(WorkflowNotFoundException.class, () -> {
      workflowRunService.triggerWorkflow(WORKFLOW_NAME, request);
    });
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowSystemMetadataRepository).getSystemWorkflow(eq(WORKFLOW_NAME));
  }

  @Test
  void testTriggerWorkflowFailedWhenSubmitIngestThrowsException() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(WORKFLOW_METADATA, WorkflowMetadata.class);
    final TriggerWorkflowRequest request =
        OBJECT_MAPPER.readValue(WORKFLOW_TRIGGER_REQUEST_DATA, TriggerWorkflowRequest.class);
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    doThrow(new CoreException("Failed to trigger workflow"))
        .when(workflowEngineService).triggerWorkflow(workflowEngineRequestArgumentCaptor.capture(),
        eq(createWorkflowPayload(RUN_ID, request)));
    when(dpsHeaders.getAuthorization()).thenReturn(AUTH_TOKEN);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when and then
    Assertions.assertThrows(CoreException.class, () -> {
      workflowRunService.triggerWorkflow(WORKFLOW_NAME, request);
    });

    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowEngineService)
        .triggerWorkflow(any(WorkflowEngineRequest.class), eq(createWorkflowPayload(RUN_ID, request)));
    WorkflowEngineRequest capturedWorkflowEngineRequest =
        workflowEngineRequestArgumentCaptor.getValue();
    assertThat(capturedWorkflowEngineRequest.getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.getRunId(), equalTo(RUN_ID));
    assertThat(capturedWorkflowEngineRequest.getWorkflowId(), equalTo(WORKFLOW_NAME));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getCorrelationId();
    verify(workflowRunRepository, never()).saveWorkflowRun(any());
  }

  @Test
  void testGetWorkflowRunByIdWhenExistingWorkflowRunFinished() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER
        .readValue(WORKFLOW_METADATA, WorkflowMetadata.class);
    final WorkflowRun submittedWorkflowRun = OBJECT_MAPPER
        .readValue(SUBMITTED_WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER
        .readValue(FINISHED_WORKFLOW_RUN, WorkflowRun.class);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowRun.class);

    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID)))
        .thenReturn(submittedWorkflowRun);
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(workflowEngineService.getWorkflowRunStatus(workflowEngineRequestArgumentCaptor.capture())).
        thenReturn(WorkflowStatusType.FINISHED);
    when(workflowRunRepository.updateWorkflowRun(workflowRunArgumentCaptor.capture())).
        thenReturn(finishedWorkflowRun);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when
    final WorkflowRunResponse returnedWorkflowRunResponse = workflowRunService.
        getWorkflowRunByName(WORKFLOW_NAME, RUN_ID);

    //then
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
    verify(workflowEngineService).getWorkflowRunStatus(any(WorkflowEngineRequest.class));
    WorkflowEngineRequest capturedWorkflowEngineRequest =
        workflowEngineRequestArgumentCaptor.getValue();
    assertThat(capturedWorkflowEngineRequest.getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.getWorkflowEngineExecutionDate(),
        equalTo(EXECUTION_DATE));
    verify(workflowRunRepository).updateWorkflowRun(any(WorkflowRun.class));
	  assertThat(returnedWorkflowRunResponse, equalTo(buildWorkflowRunResponse(finishedWorkflowRun)));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowId(),
        equalTo(submittedWorkflowRun.getWorkflowId()));
    assertThat(workflowRunArgumentCaptor.getValue().getSubmittedBy(),
        equalTo(submittedWorkflowRun.getSubmittedBy()));
    assertThat(workflowRunArgumentCaptor.getValue().getStartTimeStamp(),
        equalTo(submittedWorkflowRun.getStartTimeStamp()));
    assertThat(workflowRunArgumentCaptor.getValue().getRunId(),
        equalTo(submittedWorkflowRun.getRunId()));
    assertThat(workflowRunArgumentCaptor.getValue().getStatus(),
        equalTo(WorkflowStatusType.FINISHED));
  }

  @Test
  void testGetWorkflowRunByIdWhenExistingWorkflowRunRunning() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER
        .readValue(WORKFLOW_METADATA, WorkflowMetadata.class);
    final WorkflowRun submittedWorkflowRun = OBJECT_MAPPER
        .readValue(SUBMITTED_WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRun runningWorkflowRun = OBJECT_MAPPER
        .readValue(RUNNING_WORKFLOW_RUN, WorkflowRun.class);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowRun.class);

    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID))).
        thenReturn(submittedWorkflowRun);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(workflowEngineService.
        getWorkflowRunStatus(workflowEngineRequestArgumentCaptor.capture())).
        thenReturn(WorkflowStatusType.RUNNING);
    when(workflowRunRepository.updateWorkflowRun(workflowRunArgumentCaptor.capture())).
        thenReturn(runningWorkflowRun);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when
    final WorkflowRunResponse returnedWorkflowRunResponse =
        workflowRunService.getWorkflowRunByName(WORKFLOW_NAME, RUN_ID);

    //then
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
    verify(workflowEngineService).getWorkflowRunStatus(any(WorkflowEngineRequest.class));
    WorkflowEngineRequest capturedWorkflowEngineRequest =
        workflowEngineRequestArgumentCaptor.getValue();
    assertThat(capturedWorkflowEngineRequest.getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.getWorkflowEngineExecutionDate(),
        equalTo(EXECUTION_DATE));
    verify(workflowRunRepository).updateWorkflowRun(any(WorkflowRun.class));

	  assertThat(returnedWorkflowRunResponse, equalTo(buildWorkflowRunResponse(runningWorkflowRun)));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowId(),
        equalTo(submittedWorkflowRun.getWorkflowId()));
    assertThat(workflowRunArgumentCaptor.getValue().getSubmittedBy(),
        equalTo(submittedWorkflowRun.getSubmittedBy()));
    assertThat(workflowRunArgumentCaptor.getValue().getStartTimeStamp(),
        equalTo(submittedWorkflowRun.getStartTimeStamp()));
    assertThat(workflowRunArgumentCaptor.getValue().getEndTimeStamp(),
        equalTo(submittedWorkflowRun.getEndTimeStamp()));
    assertThat(workflowRunArgumentCaptor.getValue().getRunId(),
        equalTo(submittedWorkflowRun.getRunId()));
    assertThat(workflowRunArgumentCaptor.getValue().getStatus(),
        equalTo(WorkflowStatusType.RUNNING));
  }

  @Test
  public void testGetWorkflowRunByIdWhenWorkflowRunStatusUpToDate() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(WORKFLOW_METADATA, WorkflowMetadata.class);
    final WorkflowRun runningWorkflowRun = OBJECT_MAPPER.readValue(RUNNING_WORKFLOW_RUN, WorkflowRun.class);

    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID))).
        thenReturn(runningWorkflowRun);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(workflowEngineService.getWorkflowRunStatus(workflowEngineRequestArgumentCaptor.capture())).
        thenReturn(WorkflowStatusType.RUNNING);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when
    final WorkflowRunResponse returnedWorkflowRunResponse =
        workflowRunService.getWorkflowRunByName(WORKFLOW_NAME, RUN_ID);

    //then
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
    verify(workflowEngineService).getWorkflowRunStatus(any(WorkflowEngineRequest.class));
    WorkflowEngineRequest capturedWorkflowEngineRequest =
        workflowEngineRequestArgumentCaptor.getValue();
    assertThat(capturedWorkflowEngineRequest.getWorkflowName(), equalTo(WORKFLOW_NAME));
    assertThat(capturedWorkflowEngineRequest.getWorkflowEngineExecutionDate(),
        equalTo(EXECUTION_DATE));

    assertThat(returnedWorkflowRunResponse, equalTo(buildWorkflowRunResponse(runningWorkflowRun)));
  }

  @Test
  void testGetWorkflowRunByIdWhenExistingFinishedWorkflowRun() throws Exception {
    //given
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER
        .readValue(FINISHED_WORKFLOW_RUN, WorkflowRun.class);

    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID)))
        .thenReturn(finishedWorkflowRun);

    //when
    final WorkflowRunResponse returnedWorkflowRunResponse =
        workflowRunService.getWorkflowRunByName(WORKFLOW_NAME, RUN_ID);

    //then
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
	  assertThat(returnedWorkflowRunResponse, equalTo(buildWorkflowRunResponse(finishedWorkflowRun)));
  }

  @Test
  void testGetWorkflowRunByIdWhenNonExistingWorkflowRun() {
    //given
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID)))
        .thenThrow(WorkflowRunNotFoundException.class);

    //when and then
    Assertions.assertThrows(WorkflowRunNotFoundException.class, () -> {
      workflowRunService.getWorkflowRunByName(WORKFLOW_NAME, RUN_ID);
    });
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
  }

  @Test
  public void testUpdateWorkflowRunStatusRunningWithExistingWorkflowRun() throws Exception {
    //given
    final UpdateWorkflowRunRequest request = OBJECT_MAPPER
        .readValue(WORKFLOW_RUN_UPDATE_RUNNING_STATUS_REQUEST_DATA, UpdateWorkflowRunRequest.class);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor.forClass(WorkflowRun.class);
    final WorkflowRun submittedWorkflowRun = OBJECT_MAPPER.readValue(SUBMITTED_WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRun runningWorkflowRun = OBJECT_MAPPER.readValue(RUNNING_WORKFLOW_RUN, WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID))).thenReturn(submittedWorkflowRun);
    when(workflowRunRepository.updateWorkflowRun(workflowRunArgumentCaptor.capture())).thenReturn(runningWorkflowRun);

    //when
    final WorkflowRunResponse returnedWorkflowRunResponse = workflowRunService
        .updateWorkflowRunStatus(WORKFLOW_NAME, RUN_ID, request.getStatus());

    //then
    verify(workflowRunRepository).updateWorkflowRun(any(WorkflowRun.class));
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
    assertThat(workflowRunArgumentCaptor.getValue().getStatus(), equalTo(WorkflowStatusType.RUNNING));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowId(), equalTo(submittedWorkflowRun.getWorkflowId()));
    assertThat(workflowRunArgumentCaptor.getValue().getSubmittedBy(), equalTo(submittedWorkflowRun.getSubmittedBy()));
    assertThat(workflowRunArgumentCaptor.getValue().getStartTimeStamp(), equalTo(submittedWorkflowRun.getStartTimeStamp()));
    assertThat(workflowRunArgumentCaptor.getValue().getEndTimeStamp(), equalTo(submittedWorkflowRun.getEndTimeStamp()));
    assertThat(workflowRunArgumentCaptor.getValue().getRunId(), equalTo(submittedWorkflowRun.getRunId()));
    assertThat(returnedWorkflowRunResponse, equalTo(buildWorkflowRunResponse(runningWorkflowRun)));
  }

  @Test
  public void testUpdateWorkflowRunStatusFinishedWithExistingWorkflowRun() throws Exception {
    //given
    final UpdateWorkflowRunRequest request = OBJECT_MAPPER
        .readValue(WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA, UpdateWorkflowRunRequest.class);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor.forClass(WorkflowRun.class);
    final WorkflowRun submittedWorkflowRun = OBJECT_MAPPER.readValue(SUBMITTED_WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER.readValue(FINISHED_WORKFLOW_RUN, WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID))).thenReturn(submittedWorkflowRun);
    when(workflowRunRepository.updateWorkflowRun(workflowRunArgumentCaptor.capture())).thenReturn(finishedWorkflowRun);

    //when
    final WorkflowRunResponse returnedWorkflowRunResponse = workflowRunService
        .updateWorkflowRunStatus(WORKFLOW_NAME, RUN_ID, request.getStatus());

    //then
    verify(workflowRunRepository).updateWorkflowRun(any(WorkflowRun.class));
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
    assertThat(workflowRunArgumentCaptor.getValue().getStatus(), equalTo(WorkflowStatusType.FINISHED));
    assertThat(workflowRunArgumentCaptor.getValue().getWorkflowId(), equalTo(submittedWorkflowRun.getWorkflowId()));
    assertThat(workflowRunArgumentCaptor.getValue().getSubmittedBy(), equalTo(submittedWorkflowRun.getSubmittedBy()));
    assertThat(workflowRunArgumentCaptor.getValue().getStartTimeStamp(), equalTo(submittedWorkflowRun.getStartTimeStamp()));
    assertThat(workflowRunArgumentCaptor.getValue().getRunId(), equalTo(submittedWorkflowRun.getRunId()));
    assertThat(returnedWorkflowRunResponse, equalTo(buildWorkflowRunResponse(finishedWorkflowRun)));
  }

  @Test
  public void testUpdateWorkflowRunStatusWithFinishedWorkflowRun() throws Exception {
    //given
    final UpdateWorkflowRunRequest request = OBJECT_MAPPER
        .readValue(WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA, UpdateWorkflowRunRequest.class);
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER.readValue(FINISHED_WORKFLOW_RUN, WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID))).thenReturn(finishedWorkflowRun);

    //when and then
    Assertions.assertThrows(WorkflowRunCompletedException.class, () -> {
      workflowRunService.updateWorkflowRunStatus(WORKFLOW_NAME, RUN_ID, request.getStatus());
    });
    verify(workflowRunRepository).getWorkflowRun(eq(WORKFLOW_NAME), eq(RUN_ID));
  }

  @Test
  void testDeleteWorkflowRunsByWorkflowIdWithInActiveWorkflowRuns() throws Exception {
    //given
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER.readValue(FINISHED_WORKFLOW_RUN,
        WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null)))
        .thenReturn(new WorkflowRunsPage(Arrays.asList(finishedWorkflowRun, finishedWorkflowRun),
            null));
    ArgumentCaptor<List<String>> runIdListCaptor = ArgumentCaptor.forClass(List.class);
    doNothing().when(workflowRunRepository).deleteWorkflowRuns(eq(WORKFLOW_NAME),
        runIdListCaptor.capture());

    //when
    workflowRunService.deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);

    //then
    verify(workflowRunRepository)
        .getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null));
    verify(workflowRunRepository).deleteWorkflowRuns(eq(WORKFLOW_NAME), any(List.class));
    List<String> capturedRunIds = runIdListCaptor.getValue();
    for (String capturedRunId : capturedRunIds) {
      Assertions.assertEquals(finishedWorkflowRun.getRunId(), capturedRunId);
    }
  }

  @Test
  void testDeleteWorkflowRunsByWorkflowIdWithActiveWorkflowRuns() throws Exception {
    //given
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER.readValue(FINISHED_WORKFLOW_RUN,
        WorkflowRun.class);
	  final WorkflowRun submittedWorkflowRun = OBJECT_MAPPER.readValue(SUBMITTED_WORKFLOW_RUN,
        WorkflowRun.class);
    final WorkflowRun runningWorkflowRun = OBJECT_MAPPER.readValue(RUNNING_WORKFLOW_RUN,
        WorkflowRun.class);
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(WORKFLOW_METADATA,
        WorkflowMetadata.class);
    final ArgumentCaptor<WorkflowEngineRequest> requestArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowEngineRequest.class);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null)))
        .thenReturn(new WorkflowRunsPage(Arrays.asList(finishedWorkflowRun, submittedWorkflowRun),
            TEST_CURSOR));
    when(workflowRunRepository.getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(),
        eq(TEST_CURSOR))).thenReturn(new WorkflowRunsPage(Arrays.asList(submittedWorkflowRun),
        null));
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    when(workflowEngineService.getWorkflowRunStatus(requestArgumentCaptor.capture()))
        .thenReturn(WorkflowStatusType.RUNNING);
    when(workflowRunRepository.updateWorkflowRun(workflowRunArgumentCaptor.capture()))
        .thenReturn(runningWorkflowRun);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    boolean isExceptionThrown = false;
    try {
      //when
      workflowRunService.deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);
    } catch (AppException e) {
      //then
      isExceptionThrown = true;
      Assertions.assertEquals(412, e.getError().getCode());
    }

    Assertions.assertTrue(isExceptionThrown);

    verify(workflowRunRepository).getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null));
    verify(workflowRunRepository).getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(),
        eq(TEST_CURSOR));
    verify(workflowRunRepository, times(0)).deleteWorkflowRuns(eq(WORKFLOW_NAME), any(List.class));
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowEngineService).getWorkflowRunStatus(any(WorkflowEngineRequest.class));
    verify(workflowRunRepository).updateWorkflowRun(any(WorkflowRun.class));

    WorkflowEngineRequest workflowEngineRequest = requestArgumentCaptor.getValue();
    assertThat(workflowEngineRequest.getWorkflowName(),equalTo(submittedWorkflowRun.getWorkflowName()));
    assertThat(workflowEngineRequest.getExecutionTimeStamp(),
        equalTo(submittedWorkflowRun.getStartTimeStamp()));
    assertThat(workflowEngineRequest.getWorkflowEngineExecutionDate(),
        equalTo(submittedWorkflowRun.getWorkflowEngineExecutionDate()));

    WorkflowRun workflowRun = workflowRunArgumentCaptor.getValue();
    assertThat(workflowRun.getStatus(), equalTo(WorkflowStatusType.RUNNING));
    assertThat(workflowRun.getWorkflowId(), equalTo(submittedWorkflowRun.getWorkflowId()));
    assertThat(workflowRun.getSubmittedBy(), equalTo(submittedWorkflowRun.getSubmittedBy()));
    assertThat(workflowRun.getStartTimeStamp(), equalTo(submittedWorkflowRun.getStartTimeStamp()));
    assertThat(workflowRun.getEndTimeStamp(), equalTo(submittedWorkflowRun.getEndTimeStamp()));
    assertThat(workflowRun.getRunId(), equalTo(submittedWorkflowRun.getRunId()));
  }

  @Test
  void testDeleteWorkflowRunsByWorkflowIdWithFinishedWorkflowRunsInIncompleteState()
      throws Exception {
    //given
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER.readValue(FINISHED_WORKFLOW_RUN,
        WorkflowRun.class);
    final WorkflowRun runningWorkflowRun = OBJECT_MAPPER.readValue(RUNNING_WORKFLOW_RUN,
        WorkflowRun.class);
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(WORKFLOW_METADATA,
        WorkflowMetadata.class);
    final ArgumentCaptor<WorkflowEngineRequest> requestArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowEngineRequest.class);
    final ArgumentCaptor<WorkflowRun> workflowRunArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowRun.class);
    when(workflowRunRepository.getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null)))
        .thenReturn(new WorkflowRunsPage(Arrays.asList(finishedWorkflowRun, runningWorkflowRun), TEST_CURSOR));
    when(workflowRunRepository.getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(),
        eq(TEST_CURSOR))).thenReturn(new WorkflowRunsPage(Arrays.asList(runningWorkflowRun),
        null));
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    when(workflowEngineService.getWorkflowRunStatus(requestArgumentCaptor.capture()))
        .thenReturn(WorkflowStatusType.FINISHED);
    when(workflowRunRepository.updateWorkflowRun(workflowRunArgumentCaptor.capture()))
        .thenReturn(finishedWorkflowRun);
    ArgumentCaptor<List<String>> runIdListCaptor = ArgumentCaptor.forClass(List.class);
    doNothing().when(workflowRunRepository).deleteWorkflowRuns(eq(WORKFLOW_NAME),
        runIdListCaptor.capture());
    when(airflowResolver.getWorkflowEngineService(workflowMetadata))
        .thenReturn(workflowEngineService);

    //when
    workflowRunService.deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);

    //then
    verify(workflowRunRepository).getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null));
    verify(workflowRunRepository).getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(),
        eq(TEST_CURSOR));
    verify(workflowRunRepository).deleteWorkflowRuns(eq(WORKFLOW_NAME), any(List.class));
    verify(workflowMetadataRepository, times(2)).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowEngineService, times(2))
        .getWorkflowRunStatus(any(WorkflowEngineRequest.class));
    verify(workflowRunRepository, times(2))
        .updateWorkflowRun(any(WorkflowRun.class));

    List<WorkflowEngineRequest> workflowEngineRequestList = requestArgumentCaptor.getAllValues();
    for(WorkflowEngineRequest workflowEngineRequest: workflowEngineRequestList){
      assertThat(workflowEngineRequest.getWorkflowName(),equalTo(runningWorkflowRun.getWorkflowName()));
      assertThat(workflowEngineRequest.getExecutionTimeStamp(),
          equalTo(runningWorkflowRun.getStartTimeStamp()));
      assertThat(workflowEngineRequest.getWorkflowEngineExecutionDate(),
          equalTo(runningWorkflowRun.getWorkflowEngineExecutionDate()));
    }
    List<WorkflowRun> workflowRunList = workflowRunArgumentCaptor.getAllValues();
    for(WorkflowRun workflowRun: workflowRunList){
      assertThat(workflowRun.getStatus(), equalTo(WorkflowStatusType.FINISHED));
      assertThat(workflowRun.getWorkflowId(), equalTo(runningWorkflowRun.getWorkflowId()));
      assertThat(workflowRun.getSubmittedBy(), equalTo(runningWorkflowRun.getSubmittedBy()));
      assertThat(workflowRun.getStartTimeStamp(), equalTo(runningWorkflowRun.getStartTimeStamp()));
      assertThat(workflowRun.getRunId(), equalTo(runningWorkflowRun.getRunId()));
    }

  }

  @Test
  void testDeleteWorkflowRunsByWorkflowIdWithZeroWorkflowRuns() {
    //given
    when(workflowRunRepository.getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null)))
        .thenReturn(new WorkflowRunsPage(new ArrayList<>(), null));

    //when
    workflowRunService.deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);

    //then
    verify(workflowRunRepository)
        .getWorkflowRunsByWorkflowName(eq(WORKFLOW_NAME), anyInt(), eq(null));
    verify(workflowRunRepository, times(0)).deleteWorkflowRuns(eq(WORKFLOW_NAME), any(List.class));
  }

  @Test
  void testGetAllRunInstancesOfWorkflowForExistentWorkflowName() throws Exception {
    //given
    final WorkflowRun finishedWorkflowRun = OBJECT_MAPPER.readValue(FINISHED_WORKFLOW_RUN,
        WorkflowRun.class);
    final WorkflowRun submittedWorkflowRun = OBJECT_MAPPER.readValue(SUBMITTED_WORKFLOW_RUN,
        WorkflowRun.class);
    final WorkflowRun runningWorkflowRun = OBJECT_MAPPER.readValue(RUNNING_WORKFLOW_RUN,
        WorkflowRun.class);
    final List<WorkflowRun> workflowRuns =
        Arrays.asList(finishedWorkflowRun, submittedWorkflowRun, runningWorkflowRun);
    final Map<String, Object> config = new HashMap<>();
    final WorkflowMetadata workflowMetadata = mock(WorkflowMetadata.class);
    when(workflowMetadataRepository.getWorkflow(eq(WORKFLOW_NAME))).thenReturn(workflowMetadata);
    when(workflowRunRepository.getAllRunInstancesOfWorkflow(eq(WORKFLOW_NAME), eq(config))).thenReturn(workflowRuns);

    //when and then
    Assertions.assertEquals(workflowRuns, workflowRunService.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, config));
    verify(workflowRunRepository).getAllRunInstancesOfWorkflow(eq(WORKFLOW_NAME), eq(config));
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
  }

  @Test
  void testGetAllRunInstancesOfWorkflowForNonExistentWorkflowName() {
    //given
    when(workflowMetadataRepository.getWorkflow(WORKFLOW_NAME)).thenThrow(WorkflowNotFoundException.class);
    when(workflowSystemMetadataRepository.getSystemWorkflow(WORKFLOW_NAME)).thenThrow(WorkflowNotFoundException.class);

    //when and then
    Assertions.assertThrows(WorkflowNotFoundException.class, () -> {
      workflowRunService.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, new HashMap<>());
    });
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
  }

  private Map<String, Object> createWorkflowPayload(final String runId,
                                                    final TriggerWorkflowRequest request) {
    final Map<String, Object> executionContext = request.getExecutionContext();
    executionContext.put(KEY_USER_ID, USER_ID);
    final Map<String, Object> payload = new HashMap<>();
    payload.put(KEY_RUN_ID, runId);
    payload.put(KEY_AUTH_TOKEN, AUTH_TOKEN);
    payload.put(KEY_EXECUTION_CONTEXT, OBJECT_MAPPER.convertValue(executionContext, Map.class));
    payload.put(KEY_WORKFLOW_NAME, WORKFLOW_NAME);
    payload.put(KEY_CORRELATION_ID, CORRELATION_ID);
    return payload;
  }

  private WorkflowRunResponse buildWorkflowRunResponse(final WorkflowRun workflowRun) {
    if (workflowRun == null)
      return null;
    return WorkflowRunResponse.builder()
        .workflowId(workflowRun.getWorkflowId())
        .runId(workflowRun.getRunId())
        .startTimeStamp(workflowRun.getStartTimeStamp())
        .endTimeStamp(workflowRun.getEndTimeStamp())
        .submittedBy(workflowRun.getSubmittedBy())
        .status(workflowRun.getStatus())
        .build();
  }
}
