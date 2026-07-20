package org.opengroup.osdu.workflow.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.logging.AuditLogger;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowSystemMetadataRepository;
import org.opengroup.osdu.workflow.service.WorkflowManagerServiceImpl;

/** Tests for {@link WorkflowManagerServiceImpl} */
@ExtendWith(MockitoExtension.class)
class WorkflowManagerServiceTest {

  private static final String WORKFLOW_NAME = "test_dag_name";
  private static final String INVALID_WORKFLOW_NAME = "invalid-workflow-name";
  private static final String USER_EMAIL = "user@email.com";
  private static final long SEED_VERSION = 1;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String CREATE_WORKFLOW_REQUEST = "{\n" +
      "  \"workflowName\": \"HelloWorld\",\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"registrationInstructions\": {\n" +
      "    \"dagContent\": \"sample-dag-content\"\n" +
      "  }\n" +
      "}";

  private static final String CREATE_WORKFLOW_REQUEST_WITH_INVALID_WORKFLOW_NAME = "{\n" +
      "  \"workflowName\": \"\",\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"registrationInstructions\": {\n" +
      "    \"dagContent\": \"sample-dag-content\"\n" +
      "  }\n" +
      "}";

  private static final String GET_WORKFLOW_RESPONSE = "{\n" +
      "  \"workflowId\": \"2afccfb8-1351-41c6-9127-61f2d7f22ff8\",\n" +
      "  \"workflowName\": \"HelloWorld\",\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"creationTimestamp\": 1600144876028,\n" +
      "  \"createdBy\": \"user@email.com\",\n" +
      "  \"version\": 1\n" +
      "}";

  private static final String PREFIX_INPUT = "hwoello";

  @Mock
  private IWorkflowMetadataRepository workflowMetadataRepository;

  @Mock
  private IWorkflowSystemMetadataRepository workflowSystemMetadataRepository;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private IWorkflowEngineService workflowEngineService;

  @Mock
  private IAirflowResolver airflowResolver;

  @Mock
  private IWorkflowRunService workflowRunService;

  @Mock
  private AuditLogger auditLogger;

  @InjectMocks
  private WorkflowManagerServiceImpl workflowManagerService;

  @Test
  void testCreateWorkflowWithValidData() throws Exception {
    // given
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    final CreateWorkflowRequest request =
        OBJECT_MAPPER.readValue(CREATE_WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    final ArgumentCaptor<WorkflowMetadata> workflowMetadataCaptor = ArgumentCaptor
        .forClass(WorkflowMetadata.class);
    final WorkflowMetadata responseMetadata = mock(WorkflowMetadata.class);
    when(workflowMetadataRepository.createWorkflow(workflowMetadataCaptor.capture()))
        .thenReturn(responseMetadata);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(airflowResolver.getWorkflowEngineService(workflowMetadataCaptor.capture()))
        .thenReturn(workflowEngineService);
    doNothing().when(workflowEngineService)
        .createWorkflow(workflowEngineRequestArgumentCaptor.capture(),
            eq(request.getRegistrationInstructions()));

    //when
    final WorkflowMetadata returnedMetadata = workflowManagerService.createWorkflow(request);

    //then
    verify(workflowMetadataRepository).createWorkflow(any(WorkflowMetadata.class));
    verify(workflowEngineService).createWorkflow(any(WorkflowEngineRequest.class),
        eq(request.getRegistrationInstructions()));
    verify(dpsHeaders, times(1)).getUserEmail();
    assertThat(returnedMetadata, equalTo(responseMetadata));
    assertThat(workflowEngineRequestArgumentCaptor.getValue().getWorkflowName(),
        equalTo(workflowMetadataCaptor.getValue().getWorkflowName()));
    assertThat(workflowMetadataCaptor.getValue().getWorkflowName(),
        equalTo(request.getWorkflowName()));
    assertThat(workflowMetadataCaptor.getValue().getDescription(),
        equalTo(request.getDescription()));
    assertThat(workflowMetadataCaptor.getValue().getCreatedBy(), equalTo(USER_EMAIL));
    assertThat(workflowMetadataCaptor.getValue().getVersion(), equalTo(SEED_VERSION));
    assertThat(workflowMetadataCaptor.getAllValues().size(), equalTo(2));
    assertThat(workflowMetadataCaptor.getAllValues().get(0), equalTo(workflowMetadataCaptor.getAllValues().get(1)));
  }

  @Test
  void testCreateSystemWorkflowWithValidData() throws Exception {
    //given
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    final CreateWorkflowRequest request =
        OBJECT_MAPPER.readValue(CREATE_WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    final ArgumentCaptor<WorkflowMetadata> workflowMetadataCaptor = ArgumentCaptor
        .forClass(WorkflowMetadata.class);
    final WorkflowMetadata responseMetadata = mock(WorkflowMetadata.class);
    when(workflowSystemMetadataRepository.createSystemWorkflow(workflowMetadataCaptor.capture()))
        .thenReturn(responseMetadata);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(airflowResolver.getWorkflowEngineService(workflowMetadataCaptor.capture()))
        .thenReturn(workflowEngineService);
    doNothing().when(workflowEngineService)
        .createWorkflow(workflowEngineRequestArgumentCaptor.capture(),
            eq(request.getRegistrationInstructions()));

    //when
    final WorkflowMetadata returnedMetadata = workflowManagerService.createSystemWorkflow(request);

    //then
    verify(workflowSystemMetadataRepository).createSystemWorkflow(any(WorkflowMetadata.class));
    verify(workflowEngineService).createWorkflow(any(WorkflowEngineRequest.class),
        eq(request.getRegistrationInstructions()));
    verify(dpsHeaders).getUserEmail();
    assertThat(returnedMetadata, equalTo(responseMetadata));
    assertThat(workflowEngineRequestArgumentCaptor.getValue().getWorkflowName(),
        equalTo(workflowMetadataCaptor.getValue().getWorkflowName()));
    assertThat(workflowEngineRequestArgumentCaptor.getValue().isSystemWorkflow(),
        equalTo(true));
    assertThat(workflowMetadataCaptor.getValue().getWorkflowName(),
        equalTo(request.getWorkflowName()));
    assertThat(workflowMetadataCaptor.getValue().getDescription(),
        equalTo(request.getDescription()));
    assertThat(workflowMetadataCaptor.getValue().getCreatedBy(), equalTo(USER_EMAIL));
    assertThat(workflowMetadataCaptor.getValue().getVersion(), equalTo(SEED_VERSION));
    assertThat(workflowMetadataCaptor.getAllValues().size(), equalTo(2));
    assertThat(workflowMetadataCaptor.getAllValues().get(0), equalTo(workflowMetadataCaptor.getAllValues().get(1)));
  }

  @Test
  void testCreateWorkflowWithConflict() throws Exception {
    //given
    final CreateWorkflowRequest request =
        OBJECT_MAPPER.readValue(CREATE_WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    final ArgumentCaptor<WorkflowMetadata> workflowMetadataCaptor = ArgumentCaptor
        .forClass(WorkflowMetadata.class);
    final WorkflowMetadata responseMetadata = mock(WorkflowMetadata.class);
    when(workflowMetadataRepository.createWorkflow(workflowMetadataCaptor.capture()))
        .thenThrow(new ResourceConflictException("conflictId", "conflicted"));
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);

    //when and then
    Assertions.assertThrows(CoreException.class, () -> {
      workflowManagerService.createWorkflow(request);
    });
    verify(workflowMetadataRepository, times(1)).createWorkflow(any(WorkflowMetadata.class));
    verify(workflowEngineService, times(0))
        .createWorkflow(any(WorkflowEngineRequest.class),
            eq(request.getRegistrationInstructions()));
    verify(dpsHeaders, times(1)).getUserEmail();
    assertThat(workflowMetadataCaptor.getValue().getWorkflowName(),
        equalTo(request.getWorkflowName()));
    assertThat(workflowMetadataCaptor.getValue().getDescription(),
        equalTo(request.getDescription()));
    assertThat(workflowMetadataCaptor.getValue().getCreatedBy(), equalTo(USER_EMAIL));
    assertThat(workflowMetadataCaptor.getValue().getVersion(), equalTo(SEED_VERSION));
  }

  @Test
  void testCreateSystemWorkflowWithConflict() throws Exception {
    //given
    final CreateWorkflowRequest request =
        OBJECT_MAPPER.readValue(CREATE_WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    final ArgumentCaptor<WorkflowMetadata> workflowMetadataCaptor = ArgumentCaptor
        .forClass(WorkflowMetadata.class);
    final WorkflowMetadata responseMetadata = mock(WorkflowMetadata.class);
    when(workflowSystemMetadataRepository.createSystemWorkflow(workflowMetadataCaptor.capture()))
        .thenThrow(new ResourceConflictException("conflictId", "conflicted"));
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);

    //when and then
    Assertions.assertThrows(CoreException.class, () -> {
      workflowManagerService.createSystemWorkflow(request);
    });
    verify(workflowSystemMetadataRepository, times(1)).createSystemWorkflow(any(WorkflowMetadata.class));
    verify(workflowEngineService, times(0))
        .createWorkflow(any(WorkflowEngineRequest.class),
            eq(request.getRegistrationInstructions()));
    verify(dpsHeaders, times(1)).getUserEmail();
    assertThat(workflowMetadataCaptor.getValue().getWorkflowName(),
        equalTo(request.getWorkflowName()));
    assertThat(workflowMetadataCaptor.getValue().getDescription(),
        equalTo(request.getDescription()));
    assertThat(workflowMetadataCaptor.getValue().getCreatedBy(), equalTo(USER_EMAIL));
    assertThat(workflowMetadataCaptor.getValue().getVersion(), equalTo(SEED_VERSION));
  }

  @Test
  public void testCreateWorkflowWithInvalidWorkflowName() throws Exception {
    //given
    final CreateWorkflowRequest request =
        OBJECT_MAPPER.readValue(CREATE_WORKFLOW_REQUEST_WITH_INVALID_WORKFLOW_NAME, CreateWorkflowRequest.class);

    //when and then
    Assertions.assertThrows(BadRequestException.class, () -> {
      workflowManagerService.createWorkflow(request);
    });
  }

  @Test
  public void testCreateSystemWorkflowWithInvalidWorkflowName() throws Exception {
    //given
    final CreateWorkflowRequest request =
        OBJECT_MAPPER.readValue(CREATE_WORKFLOW_REQUEST_WITH_INVALID_WORKFLOW_NAME, CreateWorkflowRequest.class);

    //when and then
    Assertions.assertThrows(BadRequestException.class, () -> {
      workflowManagerService.createSystemWorkflow(request);
    });
  }

  @Test
  void testGetWorkflowByIdWithExistingWorkflow() {
    //given
    final WorkflowMetadata responseMetadata = mock(WorkflowMetadata.class);
    final ArgumentCaptor<String> workflowIdCaptor = ArgumentCaptor.forClass(String.class);
    when(workflowMetadataRepository.getWorkflow(workflowIdCaptor.capture()))
        .thenReturn(responseMetadata);

    //when
    final WorkflowMetadata returnedMetadata = workflowManagerService
        .getWorkflowByName(WORKFLOW_NAME);

    //then
    verify(workflowMetadataRepository).getWorkflow(anyString());
    assertThat(returnedMetadata, equalTo(responseMetadata));
    assertThat(workflowIdCaptor.getValue(), equalTo(WORKFLOW_NAME));
  }

  @Test
  void testGetWorkflowByIdWithNonExistingPrivateWorkflowExistingSystemWorkflow() {
    //given
    final WorkflowMetadata responseMetadata = mock(WorkflowMetadata.class);
    final ArgumentCaptor<String> workflowIdCaptor = ArgumentCaptor.forClass(String.class);
    when(workflowMetadataRepository.getWorkflow(workflowIdCaptor.capture()))
        .thenThrow(WorkflowNotFoundException.class);
    when(workflowSystemMetadataRepository.getSystemWorkflow(workflowIdCaptor.capture()))
        .thenReturn(responseMetadata);

    //when
    final WorkflowMetadata returnedMetadata = workflowManagerService
        .getWorkflowByName(WORKFLOW_NAME);

    //then
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowSystemMetadataRepository).getSystemWorkflow(eq(WORKFLOW_NAME));
    assertThat(returnedMetadata, equalTo(responseMetadata));
    assertThat(workflowIdCaptor.getValue(), equalTo(WORKFLOW_NAME));
  }

  @Test
  void testGetWorkflowByIdWithNonExistingWorkflow() {
    //given
    final ArgumentCaptor<String> workflowIdCaptor = ArgumentCaptor.forClass(String.class);
    when(workflowMetadataRepository.getWorkflow(workflowIdCaptor.capture()))
        .thenThrow(WorkflowNotFoundException.class);
    when(workflowSystemMetadataRepository.getSystemWorkflow(workflowIdCaptor.capture()))
        .thenThrow(WorkflowNotFoundException.class);

    //when and then
    Assertions.assertThrows(WorkflowNotFoundException.class, () -> {
      workflowManagerService.getWorkflowByName(WORKFLOW_NAME);
    });
    verify(workflowMetadataRepository).getWorkflow(eq(WORKFLOW_NAME));
    verify(workflowSystemMetadataRepository).getSystemWorkflow(eq(WORKFLOW_NAME));
    assertThat(workflowIdCaptor.getValue(), equalTo(WORKFLOW_NAME));
  }

  @Test
  void testDeleteWorkflowWithValidId() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(GET_WORKFLOW_RESPONSE,
        WorkflowMetadata.class);
    when(workflowMetadataRepository.getWorkflow(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    doNothing().when(workflowRunService).deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);
    doNothing().when(workflowMetadataRepository).deleteWorkflow(WORKFLOW_NAME);
    final ArgumentCaptor<WorkflowEngineRequest> workflowEngineRequestCaptor =
        ArgumentCaptor.forClass(WorkflowEngineRequest.class);
    when(airflowResolver.getWorkflowEngineService(workflowMetadata)).thenReturn(workflowEngineService);
    doNothing().when(workflowEngineService).deleteWorkflow(workflowEngineRequestCaptor.capture());

    //when
    workflowManagerService.deleteWorkflow(WORKFLOW_NAME);

    //then
    verify(workflowMetadataRepository).deleteWorkflow(WORKFLOW_NAME);
    verify(workflowRunService).deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);
    verify(workflowEngineService).deleteWorkflow(any());
    assertThat(workflowEngineRequestCaptor.getValue().getWorkflowName(), equalTo(WORKFLOW_NAME));
  }

  @Test
  public void testDeleteWorkflowWithInvalidId() {
    //given
    when(workflowMetadataRepository.getWorkflow(INVALID_WORKFLOW_NAME))
        .thenThrow(new WorkflowNotFoundException("not found"));

    //when and then
    Assertions.assertThrows(WorkflowNotFoundException.class, () -> {
      workflowManagerService.deleteWorkflow(INVALID_WORKFLOW_NAME);
    });

    verify(workflowMetadataRepository).getWorkflow(INVALID_WORKFLOW_NAME);
    verify(workflowMetadataRepository, times(0)).deleteWorkflow(INVALID_WORKFLOW_NAME);
    verify(workflowRunService, times(0)).deleteWorkflowRunsByWorkflowName(INVALID_WORKFLOW_NAME);
    verify(workflowEngineService, times(0)).deleteWorkflow(any(WorkflowEngineRequest.class));
  }

  @Test
  public void testDeleteWorkflowIfException() throws Exception {
    //given
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(GET_WORKFLOW_RESPONSE,
        WorkflowMetadata.class);
    when(workflowMetadataRepository.getWorkflow(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    doThrow(new AppException(419, "error", "error"))
        .when(workflowRunService).deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);

    //when and then
    Assertions.assertThrows(AppException.class, () -> {
      workflowManagerService.deleteWorkflow(WORKFLOW_NAME);
    });

    verify(workflowMetadataRepository).getWorkflow(WORKFLOW_NAME);
    verify(workflowMetadataRepository, times(0)).deleteWorkflow(WORKFLOW_NAME);
    verify(workflowRunService).deleteWorkflowRunsByWorkflowName(WORKFLOW_NAME);
    verify(workflowEngineService, times(0)).deleteWorkflow(any(WorkflowEngineRequest.class));
  }

   @Test
   public void testGetAllWorkflowsSuccess() {
    //given
     final List<WorkflowMetadata> mockedWorkflowMetadataList = mock(List.class);
     final List<WorkflowMetadata> mockedSystemWorkflowMetadataList = mock(List.class);
     when(workflowMetadataRepository.getAllWorkflowForTenant(eq(PREFIX_INPUT))).
         thenReturn(mockedWorkflowMetadataList);
     when(workflowSystemMetadataRepository.getAllSystemWorkflow(eq(PREFIX_INPUT))).
         thenReturn(mockedSystemWorkflowMetadataList);

     //when
     List<WorkflowMetadata> responseWorkflowMetadataList =
         workflowManagerService.getAllWorkflowForTenant(PREFIX_INPUT);

     //then
     verify(workflowMetadataRepository).getAllWorkflowForTenant(eq(PREFIX_INPUT));
     verify(mockedWorkflowMetadataList).addAll(eq(mockedSystemWorkflowMetadataList));
     verify(workflowSystemMetadataRepository).getAllSystemWorkflow(eq(PREFIX_INPUT));
     assertThat(responseWorkflowMetadataList, equalTo(mockedWorkflowMetadataList));
   }
}
