package org.opengroup.osdu.workflow.provider.azure.repository;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.azure.config.AzureWorkflowEngineConfig;
import org.opengroup.osdu.workflow.provider.azure.config.CosmosConfig;
import org.opengroup.osdu.workflow.provider.azure.model.WorkflowMetadataDoc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WorkflowMetadataRepository}
 */
@ExtendWith(MockitoExtension.class)
public class WorkflowMetadataRepositoryTest {
  private static final String PARTITION_ID = "someId";
  private static final String DATABASE_NAME = "someDbName";
  private static final String WORKFLOW_METADATA_COLLECTION = "someCollection";
  private static final String WORKFLOW_NAME = "HelloWorld";
  private static final String INPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT = "{\n" +
      "    \"workflowId\": \"foo\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true,\n" +
      "    \"dagContent\": \"sample-dag-content\"\n" +
      "    },\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";

  private static final String INPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT = "{\n" +
      "    \"workflowId\": \"foo\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true\n" +
      "    },\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";

  private static final String INPUT_WORKFLOW_METADATA_WITH_EMPTY_DAG_CONTENT = "{\n" +
      "    \"workflowId\": \"foo\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true,\n" +
      "    \"dagContent\": \"\"\n" +
      "    },\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";

  private static final String OUTPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT = "{\n" +
      "    \"workflowId\": \"HelloWorld\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true\n" +
      "    },\n" +
      "    \"isDeployedThroughWorkflowService\": true,\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";

  private static final String OUTPUT_GET_WORKFLOW_METADATA_WITH_DAG_CONTENT = "{\n" +
      "    \"workflowId\": \"HelloWorld\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true\n" +
      "    },\n" +
      "    \"isDeployedThroughWorkflowService\": true,\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";
  private static final String OUTPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT = "{\n" +
      "    \"workflowId\": \"HelloWorld\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true\n" +
      "    },\n" +
      "    \"isDeployedThroughWorkflowService\": false,\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";
  private static final String WORKFLOW_METADATA_DOC_WITH_DAG_CONTENT = "{\n" +
      "    \"id\": \"HelloWorld\",\n" +
      "    \"partitionKey\": \"HelloWorld\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true\n" +
      "    },\n" +
      "    \"isRegisteredByWorkflowService\": true,\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";

  private static final String WORKFLOW_METADATA_DOC_WITHOUT_OR_EMPTY_DAG_CONTENT = "{\n" +
      "    \"id\": \"HelloWorld\",\n" +
      "    \"partitionKey\": \"HelloWorld\",\n" +
      "    \"workflowName\": \"HelloWorld\",\n" +
      "    \"description\": \"This is a test workflow\",\n" +
      "    \"registrationInstructions\": {\n" +
      "    \"concurrentWorkflowRun\": 5,\n" +
      "    \"concurrentTaskRun\": 5,\n" +
      "    \"active\": true\n" +
      "    },\n" +
      "    \"isRegisteredByWorkflowService\": false,\n" +
      "    \"creationTimestamp\": 1600144876028,\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"version\": 1\n" +
      "}";
  private static final String PREFIX_VALUE = "Hello";

  private static final String SQL_QUERY_SPEC_QUERY_TEXT_WITH_PREFIX = "SELECT * FROM c " +
      "where STARTSWITH(c.workflowName, @prefix, true) " +
      "ORDER BY c._ts DESC";
  private static final String SQL_QUERY_SPEC_QUERY_TEXT_WITHOUT_PREFIX = "SELECT * FROM c " +
      "ORDER BY c._ts DESC";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String WORKFLOW_METADATA_CACHE_KEY = String.format("%s-%s", PARTITION_ID, WORKFLOW_NAME);

  @Mock
  private CosmosConfig cosmosConfig;

  @Mock
  private AzureWorkflowEngineConfig workflowEngineConfig;

  @Mock
  private CosmosStore cosmosStore;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private JaxRsDpsLog jaxRsDpsLog;

  @Mock
  private ICache<String, WorkflowMetadata> workflowMetadataCache;

  @InjectMocks
  private WorkflowMetadataRepository workflowMetadataRepository;

  @Test
  public void testCreateWorkflowWithDAGContent() throws Exception {
    when(workflowEngineConfig.getIgnoreDagContent()).thenReturn(false);
    final WorkflowMetadata inputWorkflowMetadata = OBJECT_MAPPER.readValue(INPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadata expectedOutputWorkflowMetadata = OBJECT_MAPPER.readValue(OUTPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadataDoc expectedDocToBeStored =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITH_DAG_CONTENT, WorkflowMetadataDoc.class);
    testCreateWorkflow(inputWorkflowMetadata, expectedOutputWorkflowMetadata, expectedDocToBeStored);
  }

  @Test
  public void testCreateWorkflowWithDAGContent_whenIgnoreDagContentIsTrue_thenThrowsError() throws Exception {
    when(workflowEngineConfig.getIgnoreDagContent()).thenReturn(true);
    final WorkflowMetadata inputWorkflowMetadata = OBJECT_MAPPER.readValue(INPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    Assertions.assertThrows(AppException.class, () -> {
      workflowMetadataRepository.createWorkflow(inputWorkflowMetadata);
    });
    verify(workflowEngineConfig, times(1)).getIgnoreDagContent();
  }

  @Test
  public void testCreateWorkflowWithoutDAGContent_whenIgnoreDagContentIsTrue_thenSuccess() throws Exception {
    when(workflowEngineConfig.getIgnoreDagContent()).thenReturn(true);
    final WorkflowMetadata inputWorkflowMetadata = OBJECT_MAPPER.readValue(INPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadata expectedOutputWorkflowMetadata = OBJECT_MAPPER.readValue(OUTPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadataDoc expectedDocToBeStored =
            OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITHOUT_OR_EMPTY_DAG_CONTENT, WorkflowMetadataDoc.class);
    testCreateWorkflow(inputWorkflowMetadata, expectedOutputWorkflowMetadata, expectedDocToBeStored);
  }


  @Test
  public void testCreateWorkflowWithoutDAGContent_whenIgnoreDagContentIsFalse_thenSuccess() throws Exception {
    when(workflowEngineConfig.getIgnoreDagContent()).thenReturn(false);
    final WorkflowMetadata inputWorkflowMetadata = OBJECT_MAPPER.readValue(INPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadata expectedOutputWorkflowMetadata = OBJECT_MAPPER.readValue(OUTPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadataDoc expectedDocToBeStored =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITHOUT_OR_EMPTY_DAG_CONTENT, WorkflowMetadataDoc.class);
    testCreateWorkflow(inputWorkflowMetadata, expectedOutputWorkflowMetadata, expectedDocToBeStored);
  }

  @Test
  public void testCreateWorkflowWithEmptyDAGContent() throws Exception {
    final WorkflowMetadata inputWorkflowMetadata = OBJECT_MAPPER.readValue(INPUT_WORKFLOW_METADATA_WITH_EMPTY_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadata expectedOutputWorkflowMetadata = OBJECT_MAPPER.readValue(OUTPUT_WORKFLOW_METADATA_WITHOUT_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadataDoc expectedDocToBeStored =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITHOUT_OR_EMPTY_DAG_CONTENT, WorkflowMetadataDoc.class);
    testCreateWorkflow(inputWorkflowMetadata, expectedOutputWorkflowMetadata, expectedDocToBeStored);
  }

  private void testCreateWorkflow(WorkflowMetadata inputWorkflowMetadata,
                                  WorkflowMetadata expectedOutputWorkflowMetadata,
                                  WorkflowMetadataDoc expectedDocToBeStored) {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doNothing().when(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION), eq(WORKFLOW_NAME), eq(expectedDocToBeStored));
    final WorkflowMetadata response = workflowMetadataRepository.createWorkflow(inputWorkflowMetadata);
    verify(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION), eq(WORKFLOW_NAME), eq(expectedDocToBeStored));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders).getPartitionId();
    assertThat(response, equalTo(expectedOutputWorkflowMetadata));
  }

  @Test
  public void testCreateWorkflowWithExistingId() throws Exception {
    final WorkflowMetadata inputWorkflowMetadata = OBJECT_MAPPER.readValue(INPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadataDoc workflowMetadataDoc =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITH_DAG_CONTENT, WorkflowMetadataDoc.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doThrow(new AppException(409, "conflict", "conflict")).when(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION), eq(WORKFLOW_NAME), eq(workflowMetadataDoc));
    boolean isExceptionThrown = false;

    try {
      workflowMetadataRepository.createWorkflow(inputWorkflowMetadata);
    } catch (ResourceConflictException r) {
      isExceptionThrown = true;
    }

    assertThat(isExceptionThrown, equalTo(true));
    verify(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION), eq(WORKFLOW_NAME), eq(workflowMetadataDoc));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders).getPartitionId();
  }

  @Test
  public void testGetWorkflowWithExistingWorkflowId() throws Exception {
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(OUTPUT_GET_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    final WorkflowMetadataDoc workflowMetadataDoc =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITH_DAG_CONTENT, WorkflowMetadataDoc.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(cosmosStore.findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        eq(WORKFLOW_NAME), eq(WORKFLOW_NAME), eq(WorkflowMetadataDoc.class)))
        .thenReturn(Optional.of(workflowMetadataDoc));
    final WorkflowMetadata response = workflowMetadataRepository.getWorkflow(WORKFLOW_NAME);
    verify(workflowMetadataCache).get(eq(WORKFLOW_METADATA_CACHE_KEY));
    verify(cosmosStore).findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        eq(WORKFLOW_NAME), eq(WORKFLOW_NAME), eq(WorkflowMetadataDoc.class));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders,times(2)).getPartitionId();
    assertThat(response, equalTo(workflowMetadata));
  }

  @Test
  public void testGetWorkflowWithExistingWorkflowId_workflowMetadataPresentInCache() throws Exception {
    final WorkflowMetadata workflowMetadata = OBJECT_MAPPER.readValue(OUTPUT_GET_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(workflowMetadataCache.get(eq(WORKFLOW_METADATA_CACHE_KEY))).thenReturn(workflowMetadata);
    final WorkflowMetadata response = workflowMetadataRepository.getWorkflow(WORKFLOW_NAME);
    verify(workflowMetadataCache).get(eq(WORKFLOW_METADATA_CACHE_KEY));
    verify(cosmosStore, times(0)).findItem(any(), any(), any(), any(), any(), any());
    verify(cosmosConfig, times(0)).getDatabase();
    verify(cosmosConfig, times(0)).getWorkflowMetadataCollection();
    verify(dpsHeaders,times(1)).getPartitionId();
    assertThat(response, equalTo(workflowMetadata));
  }

  @Test
  public void testGetWorkflowWithNonExistingWorkflowId() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(cosmosStore.findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        eq(WORKFLOW_NAME), eq(WORKFLOW_NAME), eq(WorkflowMetadataDoc.class)))
        .thenReturn(Optional.empty());
    Assertions.assertThrows(WorkflowNotFoundException.class, () -> {
      workflowMetadataRepository.getWorkflow(WORKFLOW_NAME);
    });
    verify(workflowMetadataCache).get(eq(WORKFLOW_METADATA_CACHE_KEY));
    verify(cosmosStore).findItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        eq(WORKFLOW_NAME), eq(WORKFLOW_NAME), eq(WorkflowMetadataDoc.class));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders, times(2)).getPartitionId();
  }

  @Test
  public void testGetAllWorkflowForTenant() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    final WorkflowMetadataDoc workflowMetadataDoc =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITH_DAG_CONTENT, WorkflowMetadataDoc.class);
    List<WorkflowMetadataDoc> workflowMetadataDocList = Arrays.asList(workflowMetadataDoc);
    ArgumentCaptor<SqlQuerySpec> sqlQuerySpecArgumentCaptor =
        ArgumentCaptor.forClass(SqlQuerySpec.class);
    when(cosmosStore.queryItems(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        sqlQuerySpecArgumentCaptor.capture(), any(CosmosQueryRequestOptions.class),
        eq(WorkflowMetadataDoc.class))).thenReturn(workflowMetadataDocList);
    List<WorkflowMetadata> responseWorkflowMetadataList =
        workflowMetadataRepository.getAllWorkflowForTenant(PREFIX_VALUE);
    verify(cosmosStore).queryItems(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class), eq(WorkflowMetadataDoc.class));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders,times(1)).getPartitionId();
    assertThat(responseWorkflowMetadataList.size(), equalTo(1));
    WorkflowMetadata workflowMetadata =
        OBJECT_MAPPER.readValue(OUTPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    assertThat(workflowMetadata, equalTo(responseWorkflowMetadataList.get(0)));
    assertThat(sqlQuerySpecArgumentCaptor.getValue().getQueryText(), equalTo(SQL_QUERY_SPEC_QUERY_TEXT_WITH_PREFIX));
  }

  @Test
  public void testGetAllWorkflowForTenantEmptyPrefix() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    final WorkflowMetadataDoc workflowMetadataDoc =
        OBJECT_MAPPER.readValue(WORKFLOW_METADATA_DOC_WITH_DAG_CONTENT, WorkflowMetadataDoc.class);
    List<WorkflowMetadataDoc> workflowMetadataDocList = Arrays.asList(workflowMetadataDoc);
    ArgumentCaptor<SqlQuerySpec> sqlQuerySpecArgumentCaptor =
        ArgumentCaptor.forClass(SqlQuerySpec.class);
    when(cosmosStore.queryItems(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        sqlQuerySpecArgumentCaptor.capture(), any(CosmosQueryRequestOptions.class),
        eq(WorkflowMetadataDoc.class))).thenReturn(workflowMetadataDocList);
    List<WorkflowMetadata> responseWorkflowMetadataList =
        workflowMetadataRepository.getAllWorkflowForTenant("");
    verify(cosmosStore).queryItems(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        any(SqlQuerySpec.class), any(CosmosQueryRequestOptions.class), eq(WorkflowMetadataDoc.class));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders,times(1)).getPartitionId();
    assertThat(responseWorkflowMetadataList.size(), equalTo(1));
    WorkflowMetadata workflowMetadata =
        OBJECT_MAPPER.readValue(OUTPUT_WORKFLOW_METADATA_WITH_DAG_CONTENT, WorkflowMetadata.class);
    assertThat(workflowMetadata, equalTo(responseWorkflowMetadataList.get(0)));
    assertThat(sqlQuerySpecArgumentCaptor.getValue().getQueryText(),
        equalTo(SQL_QUERY_SPEC_QUERY_TEXT_WITHOUT_PREFIX));
  }

  @Test
  public void testDeleteWorkflow() {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getWorkflowMetadataCollection()).thenReturn(WORKFLOW_METADATA_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doNothing().when(cosmosStore).deleteItem(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(WORKFLOW_METADATA_COLLECTION), eq(WORKFLOW_NAME), eq(WORKFLOW_NAME));
    workflowMetadataRepository.deleteWorkflow(WORKFLOW_NAME);
    verify(cosmosStore).deleteItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(WORKFLOW_METADATA_COLLECTION),
        eq(WORKFLOW_NAME), eq(WORKFLOW_NAME));
    verify(cosmosConfig).getDatabase();
    verify(cosmosConfig).getWorkflowMetadataCollection();
    verify(dpsHeaders,times(2)).getPartitionId();
    verify(workflowMetadataCache, times(2)).delete(eq(WORKFLOW_METADATA_CACHE_KEY));
  }
}
