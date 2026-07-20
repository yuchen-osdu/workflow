package org.opengroup.osdu.workflow.provider.azure.repository;

import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.WorkflowRunNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import org.opengroup.osdu.workflow.provider.azure.config.CosmosConfig;
import org.opengroup.osdu.workflow.provider.azure.consts.WorkflowRunConstants;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IActiveDagRunsCache;
import org.opengroup.osdu.workflow.provider.azure.model.WorkflowRunDoc;
import org.opengroup.osdu.workflow.provider.azure.utils.CursorUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.workflow.provider.azure.consts.CacheConstants.ACTIVE_DAG_RUNS_COUNT_CACHE_KEY;
import static org.springframework.util.Assert.doesNotContain;

/**
 * Tests for {@link WorkflowRunRepository}
 */
@ExtendWith(MockitoExtension.class)
public class WorkflowRunRepositoryTest {
  private static final String PARTITION_ID = "someId";
  private static final String WORKFLOW_NAME = "test-workflow-name";
  private static final String RUN_ID = "d13f7fd0-d27e-4176-8d60-6e9aad86e347";
  private static final String DATABASE_NAME = "someDbName";
  private static final String WORKFLOW_RUN_COLLECTION = "someCollection";
  private static final String TEST_CURSOR = "dGVzdC1jdXJzb3I=";
  private static final Integer TEST_LIMIT = 100;
  private static final Long WORKFLOW_RUN_END_TIMESTAMP = 1600258424158L;
  private static final String TEST_WORKFLOW_RUN_ID_PREFIX = "test-workflow-run-prefix";
  private static final String TEST_WORKFLOW_RUN_START_DATE = "test-start-date";
  private static final String TEST_WORKFLOW_RUN_END_DATE = "test-end-date";

  private static final String WORKFLOW_RUN = "{\n" +
      "  \"workflowName\": \"test-workflow-name\",\n" +
      "  \"workflowId\": \"test-workflow-name\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"startTimeStamp\": 1600145420675,\n" +
      "  \"workflowEngineExecutionDate\": \"2020-12-05T11:36:45\",\n" +
      "  \"status\": \"submitted\",\n" +
      "  \"submittedBy\": \"user@email.com\"\n" +
      "}";
  private static final String WORKFLOW_RUN_DOC = "{\n" +
      "  \"workflowName\": \"test-workflow-name\",\n" +
      "  \"id\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"partitionKey\": \"test-workflow-name\",\n" +
      "  \"startTimeStamp\": 1600145420675,\n" +
      "  \"workflowEngineExecutionDate\": \"2020-12-05T11:36:45\",\n" +
      "  \"status\": \"SUBMITTED\",\n" +
      "  \"submittedBy\": \"user@email.com\"\n" +
      "}";
  private static final String UPDATED_WORKFLOW_RUN_DOC = "{\n" +
      "  \"workflowName\": \"test-workflow-name\",\n" +
      "  \"id\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"partitionKey\": \"test-workflow-name\",\n" +
      "  \"startTimeStamp\": 1607430997362,\n" +
      "  \"endTimeStamp\": 1600258424158,\n" +
      "  \"status\": \"FINISHED\",\n" +
      "  \"submittedBy\": \"user@email.com\"\n" +
      "}";
  private static final String UPDATED_WORKFLOW_RUN = "{\n" +
      "  \"workflowName\" : \"test-workflow-name\",\n" +
      "  \"workflowId\": \"test-workflow-name\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"startTimeStamp\": 1607430997362,\n" +
      "  \"endTimeStamp\": 1600258424158,\n" +
      "  \"status\": \"finished\",\n" +
      "  \"submittedBy\": \"user@email.com\"\n" +
      "}";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private CosmosConfig cosmosConfig;

  @Mock
  private CosmosStore cosmosStore;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private CursorUtils cursorUtils;

  @Mock
  private WorkflowTasksSharingRepository workflowTasksSharingRepository;

  @Mock
  private IActiveDagRunsCache<String, Integer> activeDagRunsCache;

  @InjectMocks
  private WorkflowRunRepository workflowRunRepository;

  @Mock
  private JaxRsDpsLog jaxRsDpsLog;

  @Test
  public void testSaveWorkflowRun() throws Exception {
    final WorkflowRun workflowRun = OBJECT_MAPPER.readValue(WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRunDoc workflowRunDoc = OBJECT_MAPPER.readValue(WORKFLOW_RUN_DOC, WorkflowRunDoc.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doNothing().when(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION), eq(WORKFLOW_NAME), eq(workflowRunDoc));
    final WorkflowRun response = workflowRunRepository.saveWorkflowRun(workflowRun);
    verify(cosmosStore, times(1))
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION), eq(WORKFLOW_NAME), eq(workflowRunDoc));
    verify(cosmosConfig, times(1)).getDatabase();
    verify(cosmosConfig, times(1)).getWorkflowRunCollection();
    verify(dpsHeaders, times(1)).getPartitionId();
    assertThat(response, equalTo(workflowRun));
  }

  @Test
  public void testGetWorkflowRunWithExistingWorkflowRun() throws Exception {
    final WorkflowRunDoc workflowRunDoc = OBJECT_MAPPER.readValue(WORKFLOW_RUN_DOC,WorkflowRunDoc.class);
    final WorkflowRun workflowRun = OBJECT_MAPPER.readValue(WORKFLOW_RUN,WorkflowRun.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(cosmosStore.findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class)))
        .thenReturn(Optional.of(workflowRunDoc));
    final WorkflowRun response = workflowRunRepository.getWorkflowRun(WORKFLOW_NAME,RUN_ID);
    verify(cosmosStore).findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowRunCollection();
    verify(dpsHeaders).getPartitionId();
    assertThat(response, equalTo(workflowRun));
  }

  @Test
  public void testGetWorkflowRunWithNonExistingWorkflowRun() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(cosmosStore.findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class)))
        .thenReturn(Optional.empty());
    Assertions.assertThrows(WorkflowRunNotFoundException.class, () -> {
      workflowRunRepository.getWorkflowRun(WORKFLOW_NAME,RUN_ID);
    });
    verify(cosmosStore, times(1)).findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class));
    verify(cosmosConfig, times(1)).getDatabase();
    verify(cosmosConfig, times(1)).getWorkflowRunCollection();
    verify(dpsHeaders, times(1)).getPartitionId();
  }

  @Test
  public void testUpdateWorkflowRunStatusWithExistingWorkflowRun() throws Exception {
    final WorkflowRun updatedWorkflowRun = OBJECT_MAPPER.readValue(UPDATED_WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRunDoc updatedWorkflowRunDoc = OBJECT_MAPPER.readValue(UPDATED_WORKFLOW_RUN_DOC, WorkflowRunDoc.class);
    final ArgumentCaptor<WorkflowRunDoc> workflowRunDocArgumentCaptor = ArgumentCaptor.forClass(WorkflowRunDoc.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doNothing().when(cosmosStore).replaceItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION), eq(RUN_ID), eq(WORKFLOW_NAME), workflowRunDocArgumentCaptor.capture());
    when(cosmosStore.findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class))).thenReturn(Optional.of(updatedWorkflowRunDoc));
    when(activeDagRunsCache.get(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY)).thenReturn(null);
    final WorkflowRun response = workflowRunRepository.updateWorkflowRun(updatedWorkflowRun);
    verify(cosmosStore).findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class));
    verify(cosmosStore).replaceItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION), eq(RUN_ID), eq(WORKFLOW_NAME), any(WorkflowRunDoc.class));
    verify(workflowTasksSharingRepository, times(1)).deleteTasksSharingInfoContainer(eq(PARTITION_ID), eq(WORKFLOW_NAME), eq(RUN_ID));
    verify(cosmosConfig,times(2)).getDatabase();
    verify(cosmosConfig, times(2)).getWorkflowRunCollection();
    verify(dpsHeaders, times(3)).getPartitionId();
    verify(activeDagRunsCache).get(eq(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY));
    assertThat(workflowRunDocArgumentCaptor.getValue().getStatus(), equalTo(response.getStatus().toString()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getId(), equalTo(response.getRunId()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getWorkflowName(), equalTo(response.getWorkflowId()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getSubmittedBy(), equalTo(response.getSubmittedBy()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getEndTimeStamp(), equalTo(WORKFLOW_RUN_END_TIMESTAMP));
  }

  @Test
  public void testUpdateWorkflowRunStatusWithExistingWorkflowRunAndActiveDagRunsCacheContainsActiveDagRunsCount() throws Exception {
    final WorkflowRun updatedWorkflowRun = OBJECT_MAPPER.readValue(UPDATED_WORKFLOW_RUN, WorkflowRun.class);
    final WorkflowRunDoc updatedWorkflowRunDoc = OBJECT_MAPPER.readValue(UPDATED_WORKFLOW_RUN_DOC, WorkflowRunDoc.class);
    final ArgumentCaptor<WorkflowRunDoc> workflowRunDocArgumentCaptor = ArgumentCaptor.forClass(WorkflowRunDoc.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doNothing().when(cosmosStore).replaceItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION), eq(RUN_ID), eq(WORKFLOW_NAME), workflowRunDocArgumentCaptor.capture());
    when(cosmosStore.findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class))).thenReturn(Optional.of(updatedWorkflowRunDoc));
    when(activeDagRunsCache.get(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY)).thenReturn(1);
    final WorkflowRun response = workflowRunRepository.updateWorkflowRun(updatedWorkflowRun);
    verify(cosmosStore).findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        eq(RUN_ID), eq(WORKFLOW_NAME), eq(WorkflowRunDoc.class));
    verify(cosmosStore).replaceItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION), eq(RUN_ID), eq(WORKFLOW_NAME), any(WorkflowRunDoc.class));
    verify(workflowTasksSharingRepository, times(1)).deleteTasksSharingInfoContainer(eq(PARTITION_ID), eq(WORKFLOW_NAME), eq(RUN_ID));
    verify(cosmosConfig,times(2)).getDatabase();
    verify(cosmosConfig, times(2)).getWorkflowRunCollection();
    verify(dpsHeaders, times(3)).getPartitionId();
    verify(activeDagRunsCache).get(eq(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY));
    verify(activeDagRunsCache).decrementKey(eq(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY));
    assertThat(workflowRunDocArgumentCaptor.getValue().getStatus(), equalTo(response.getStatus().toString()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getId(), equalTo(response.getRunId()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getWorkflowName(), equalTo(response.getWorkflowId()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getSubmittedBy(), equalTo(response.getSubmittedBy()));
    assertThat(workflowRunDocArgumentCaptor.getValue().getEndTimeStamp(), equalTo(WORKFLOW_RUN_END_TIMESTAMP));
  }

  @Test
  public void testGetWorkflowRunsByWorkflowIdWithValidWorkflowId() throws Exception {
    final WorkflowRunDoc workflowRunDoc = OBJECT_MAPPER.readValue(WORKFLOW_RUN_DOC,
        WorkflowRunDoc.class);
    List<WorkflowRun> workflowRunList = verifyAndGetWorkflowRunsByWorkflowName(WORKFLOW_NAME, null,
        Arrays.asList(workflowRunDoc));
    Assertions.assertEquals(1, workflowRunList.size());
    WorkflowRun returnedWorkflowRun = workflowRunList.get(0);
    final WorkflowRun expectedWorkflowRun =
        OBJECT_MAPPER.readValue(WORKFLOW_RUN, WorkflowRun.class);
    Assertions.assertEquals(expectedWorkflowRun, returnedWorkflowRun);
  }

  @Test
  public void testGetWorkflowRunsByWorkflowIdWithValidWorkflowIdWithCursor() throws Exception {
    final WorkflowRunDoc workflowRunDoc = OBJECT_MAPPER.readValue(WORKFLOW_RUN_DOC,
        WorkflowRunDoc.class);
    List<WorkflowRun> workflowRunList = verifyAndGetWorkflowRunsByWorkflowName(WORKFLOW_NAME,
        TEST_CURSOR, Arrays.asList(workflowRunDoc));
    Assertions.assertEquals(1, workflowRunList.size());
    WorkflowRun returnedWorkflowRun = workflowRunList.get(0);
    final WorkflowRun expectedWorkflowRun =
        OBJECT_MAPPER.readValue(WORKFLOW_RUN, WorkflowRun.class);
    Assertions.assertEquals(expectedWorkflowRun, returnedWorkflowRun);
  }

  @Test
  public void testGetWorkflowRunsByWorkflowIdWithInValidWorkflowId() throws Exception {
    List<WorkflowRun> workflowRunList = verifyAndGetWorkflowRunsByWorkflowName("invalid-workflow-id",
        null, new ArrayList<>());
    Assertions.assertEquals(0, workflowRunList.size());
  }

  @Test
  public void testDeleteWorkflowRuns() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    ArgumentCaptor<String> runIdCaptor = ArgumentCaptor.forClass(String.class);
    doNothing().when(cosmosStore).deleteItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION), runIdCaptor.capture(), eq(WORKFLOW_NAME));

    List<String> runIds = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    workflowRunRepository.deleteWorkflowRuns(WORKFLOW_NAME, runIds);
    verify(cosmosStore, times(runIds.size())).deleteItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION), anyString(), eq(WORKFLOW_NAME));
    List<String> capturedRunIds = runIdCaptor.getAllValues();
    for(int i = 0; i < runIds.size(); i++) {
      Assertions.assertEquals(runIds.get(i), capturedRunIds.get(i));
    }
    verify(cosmosConfig, times(runIds.size())).getDatabase();
    verify(cosmosConfig, times(runIds.size())).getWorkflowRunCollection();
    verify(dpsHeaders, times(runIds.size())).getPartitionId();
  }

  @Test
  public void testGetAllRunInstancesOfWorkflow_givenValidParams() {
    Page<WorkflowRunDoc> pagedWorkflowRunDoc = mock(Page.class);
    CosmosStorePageRequest cosmosStorePageRequest = mock(CosmosStorePageRequest.class);

    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(cursorUtils.decodeCosmosCursor(eq(TEST_CURSOR))).thenReturn(TEST_CURSOR);
    when(pagedWorkflowRunDoc.getPageable()).thenReturn(cosmosStorePageRequest);

    GetAllRunInstancesParams getAllRunInstancesParams = GetAllRunInstancesParams.builder()
        .limit(TEST_LIMIT)
        .cursor(TEST_CURSOR)
        .prefix(TEST_WORKFLOW_RUN_ID_PREFIX)
        .startDate(TEST_WORKFLOW_RUN_START_DATE)
        .endDate(TEST_WORKFLOW_RUN_END_DATE)
        .build();

    ArgumentCaptor<SqlQuerySpec> sqlQuerySpecArgumentCaptor = ArgumentCaptor.forClass(SqlQuerySpec.class);

    when(cosmosStore.queryItemsPage(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION),
        any(SqlQuerySpec.class),
        eq(WorkflowRunDoc.class),
        eq(TEST_LIMIT),
        eq(TEST_CURSOR))).thenReturn(pagedWorkflowRunDoc);
    workflowRunRepository.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, getAllRunInstancesParams.getParams());

    verify(cosmosStore, times(1)).queryItemsPage(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION),
        sqlQuerySpecArgumentCaptor.capture(),
        eq(WorkflowRunDoc.class),
        eq(getAllRunInstancesParams.getLimit()),
        eq(getAllRunInstancesParams.getCursor())
    );

    String queryText = sqlQuerySpecArgumentCaptor.getValue().getQueryText();
    assertThat(queryText, containsString(String.format("startswith(c.id, '%s')", TEST_WORKFLOW_RUN_ID_PREFIX)));
    assertThat(queryText, containsString(String.format("c.startTimeStamp >= %s", TEST_WORKFLOW_RUN_START_DATE)));
    assertThat(queryText, containsString(String.format("c.endTimeStamp <= %s", TEST_WORKFLOW_RUN_END_DATE)));
    verify(cosmosConfig,times(1)).getDatabase();
    verify(cosmosConfig, times(1)).getWorkflowRunCollection();
    verify(dpsHeaders, times(1)).getPartitionId();
    verify(cursorUtils, times(1)).decodeCosmosCursor(eq(TEST_CURSOR));
  }

  @Test
  public void testGetAllRunInstancesOfWorkflow_whenInvalidPrefixProvided_thenThrowsException() {
    GetAllRunInstancesParams getAllRunInstancesParams = GetAllRunInstancesParams.builder()
        .limit(TEST_LIMIT)
        .cursor(TEST_CURSOR)
        .prefix(WorkflowRunConstants.INVALID_WORKFLOW_RUN_PREFIX)
        .startDate(TEST_WORKFLOW_RUN_START_DATE)
        .endDate(TEST_WORKFLOW_RUN_END_DATE)
        .build();

    try {
      workflowRunRepository.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, getAllRunInstancesParams.getParams());
    } catch (AppException e) {
      AppError error = e.getError();
      assertEquals(error.getCode(), HttpStatus.SC_BAD_REQUEST);
      assertEquals(error.getReason(), "Invalid prefix");
      assertEquals(error.getMessage(), "Prefix must not contain the word 'backfill'");
    }
  }

  @Test
  public void testGetAllRunInstancesOfWorkflow_whenNoLimitProvided_thenUseDefaultLimit() {
    Page<WorkflowRunDoc> pagedWorkflowRunDoc = mock(Page.class);
    CosmosStorePageRequest cosmosStorePageRequest = mock(CosmosStorePageRequest.class);

    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(pagedWorkflowRunDoc.getPageable()).thenReturn(cosmosStorePageRequest);

    GetAllRunInstancesParams getAllRunInstancesParams = GetAllRunInstancesParams.builder().build();

    ArgumentCaptor<SqlQuerySpec> sqlQuerySpecArgumentCaptor = ArgumentCaptor.forClass(SqlQuerySpec.class);

    when(cosmosStore.queryItemsPage(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION),
        any(SqlQuerySpec.class),
        eq(WorkflowRunDoc.class),
        eq(WorkflowRunConstants.DEFAULT_WORKFLOW_RUNS_LIMIT),
        eq(null))).thenReturn(pagedWorkflowRunDoc);
    workflowRunRepository.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, getAllRunInstancesParams.getParams());

    verify(cosmosStore, times(1)).queryItemsPage(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION),
        sqlQuerySpecArgumentCaptor.capture(),
        eq(WorkflowRunDoc.class),
        eq(WorkflowRunConstants.DEFAULT_WORKFLOW_RUNS_LIMIT),
        eq(getAllRunInstancesParams.getCursor())
    );

    verify(cosmosConfig,times(1)).getDatabase();
    verify(cosmosConfig, times(1)).getWorkflowRunCollection();
    verify(dpsHeaders, times(1)).getPartitionId();
  }

  @Test
  public void testGetAllRunInstancesOfWorkflow_whenGivenInvalidLimit_thenThrowsException() {
    final Integer INVALID_LIMIT = WorkflowRunConstants.MAX_WORKFLOW_RUNS_LIMIT + 1;

    GetAllRunInstancesParams getAllRunInstancesParams = GetAllRunInstancesParams.builder()
        .limit(INVALID_LIMIT)
        .startDate(TEST_WORKFLOW_RUN_START_DATE)
        .endDate(TEST_WORKFLOW_RUN_END_DATE)
        .build();

    try {
      workflowRunRepository.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, getAllRunInstancesParams.getParams());
    } catch (AppException e) {
      AppError error = e.getError();
      assertEquals(error.getCode(), HttpStatus.SC_BAD_REQUEST);
      assertEquals(error.getReason(), "Invalid limit");
      assertEquals(error.getMessage(), String.format("Maximum limit allowed is %s", WorkflowRunConstants.MAX_WORKFLOW_RUNS_LIMIT));
    }
  }

  @Test
  public void testGetAllRunInstancesOfWorkflow_whenGivenInvalidLimitAndInvalidPrefix_thenThrowsException() {
    final Integer INVALID_LIMIT = WorkflowRunConstants.MAX_WORKFLOW_RUNS_LIMIT + 1;
    GetAllRunInstancesParams getAllRunInstancesParams = GetAllRunInstancesParams.builder()
        .limit(INVALID_LIMIT)
        .prefix(WorkflowRunConstants.INVALID_WORKFLOW_RUN_PREFIX)
        .build();

    try {
      workflowRunRepository.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, getAllRunInstancesParams.getParams());
    } catch (AppException e) {
      AppError error = e.getError();
      assertEquals(error.getCode(), HttpStatus.SC_BAD_REQUEST);
      assertEquals(error.getReason(), "Invalid prefix");
      assertEquals(error.getMessage(), "Prefix must not contain the word 'backfill'");
    }
  }

  @Test
  public void testGetAllRunInstancesOfWorkflow_givenOnlyStartDateParam_thenOtherParamsShouldNotBePresentInQueryText() {
    Page<WorkflowRunDoc> pagedWorkflowRunDoc = mock(Page.class);
    CosmosStorePageRequest cosmosStorePageRequest = mock(CosmosStorePageRequest.class);

    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(pagedWorkflowRunDoc.getPageable()).thenReturn(cosmosStorePageRequest);

    GetAllRunInstancesParams getAllRunInstancesParams = GetAllRunInstancesParams.builder()
        .startDate(TEST_WORKFLOW_RUN_START_DATE)
        .build();

    ArgumentCaptor<SqlQuerySpec> sqlQuerySpecArgumentCaptor = ArgumentCaptor.forClass(SqlQuerySpec.class);

    when(cosmosStore.queryItemsPage(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION),
        any(SqlQuerySpec.class),
        eq(WorkflowRunDoc.class),
        eq(WorkflowRunConstants.DEFAULT_WORKFLOW_RUNS_LIMIT),
        eq(null))).thenReturn(pagedWorkflowRunDoc);
    workflowRunRepository.getAllRunInstancesOfWorkflow(WORKFLOW_NAME, getAllRunInstancesParams.getParams());

    verify(cosmosStore, times(1)).queryItemsPage(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_RUN_COLLECTION),
        sqlQuerySpecArgumentCaptor.capture(),
        eq(WorkflowRunDoc.class),
        eq(WorkflowRunConstants.DEFAULT_WORKFLOW_RUNS_LIMIT),
        eq(null)
    );

    String queryText = sqlQuerySpecArgumentCaptor.getValue().getQueryText();
    assertThat(queryText, containsString(String.format("c.startTimeStamp >= %s", TEST_WORKFLOW_RUN_START_DATE)));
    doesNotContain(queryText, "c.endTimeStamp <=", "Query text should not contain end time related query");
    doesNotContain(queryText, "startswith", "Query text should not contain prefix related query");
    verify(cosmosConfig,times(1)).getDatabase();
    verify(cosmosConfig, times(1)).getWorkflowRunCollection();
    verify(dpsHeaders, times(1)).getPartitionId();
  }

  private List<WorkflowRun> verifyAndGetWorkflowRunsByWorkflowName(String workflowId, String cursor,
                                                                   List<WorkflowRunDoc> toBeReturnedWorkflowRunDocs) {
    if(cursor != null) {
      when(cursorUtils.decodeCosmosCursor(eq(cursor))).thenReturn(cursor);
    }
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowRunCollection()).thenReturn(WORKFLOW_RUN_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    ArgumentCaptor<SqlQuerySpec> sqlQuerySpecArgumentCaptor =
        ArgumentCaptor.forClass(SqlQuerySpec.class);
    Page<WorkflowRunDoc> workflowRunDocPage = new PageImpl<>(toBeReturnedWorkflowRunDocs,
        CosmosStorePageRequest.of(1,1, null, Sort.unsorted()), 1);
    when(cosmosStore.queryItemsPage(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        sqlQuerySpecArgumentCaptor.capture(), eq(WorkflowRunDoc.class), eq(TEST_LIMIT),
        eq(cursor))).thenReturn(workflowRunDocPage);

    WorkflowRunsPage workflowRunsPage =
        workflowRunRepository.getWorkflowRunsByWorkflowName(workflowId, TEST_LIMIT, cursor);

    verify(cosmosStore).queryItemsPage(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_RUN_COLLECTION),
        any(SqlQuerySpec.class), eq(WorkflowRunDoc.class), eq(TEST_LIMIT), eq(cursor));
    SqlQuerySpec capturedSqlQuerySpec = sqlQuerySpecArgumentCaptor.getValue();
    Assertions.assertEquals("SELECT * from c where c.partitionKey = @workflowName ORDER BY c._ts DESC",
        capturedSqlQuerySpec.getQueryText());
    Assertions.assertEquals(1, capturedSqlQuerySpec.getParameters().size());
    SqlParameter capturedSqlParameter = capturedSqlQuerySpec.getParameters().get(0);
    Assertions.assertEquals("@workflowName", capturedSqlParameter.getName());
    Assertions.assertEquals(workflowId, capturedSqlParameter.getValue(String.class));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowRunCollection();
    verify(dpsHeaders).getPartitionId();
    if(cursor != null) {
      verify(cursorUtils).decodeCosmosCursor(eq(cursor));
    }

    return workflowRunsPage.getItems();
  }
}

@Getter
@Builder
class GetAllRunInstancesParams {
  private final Integer limit;
  private final String cursor;
  private final String startDate;
  private final String endDate;
  private final String prefix;

  public Map<String, Object> getParams() {
    Map<String, Object> params = new HashMap<>();
    if (limit != null) params.put("limit", String.valueOf(limit));
    if (cursor != null) params.put("cursor", cursor);
    if (startDate != null) params.put("startDate", startDate);
    if (endDate != null) params.put("endDate", endDate);
    if (prefix != null) params.put("prefix", prefix);
    return params;
  }
}
