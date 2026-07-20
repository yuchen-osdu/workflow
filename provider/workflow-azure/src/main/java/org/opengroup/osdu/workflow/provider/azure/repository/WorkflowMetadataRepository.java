package org.opengroup.osdu.workflow.provider.azure.repository;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
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
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.opengroup.osdu.workflow.provider.azure.utils.WorkflowMetadataUtils.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WorkflowMetadataRepository implements IWorkflowMetadataRepository {
  private static final String LOGGER_NAME = WorkflowMetadataRepository.class.getName();
  private static final String KEY_DAG_CONTENT = "dagContent";

  private final CosmosConfig cosmosConfig;

  private final CosmosStore cosmosStore;

  private final DpsHeaders dpsHeaders;

  private final JaxRsDpsLog logger;

  private final AzureWorkflowEngineConfig workflowEngineConfig;

  @Qualifier("WorkflowMetadataCache")
  private final ICache<String, WorkflowMetadata> workflowMetadataCache;

  @Override
  public WorkflowMetadata createWorkflow(final WorkflowMetadata workflowMetadata) {
    String dagContent = (String) workflowMetadata.getRegistrationInstructions().get(KEY_DAG_CONTENT);
    if (workflowEngineConfig.getIgnoreDagContent()) {
      if (!StringUtils.isEmpty(dagContent)) {
        throw new AppException(HttpStatus.SC_FORBIDDEN, "Non empty dag content obtained", "Setting dag content not allowed");
      }
    }
    final WorkflowMetadataDoc workflowMetadataDoc = buildWorkflowMetadataDoc(workflowEngineConfig, workflowMetadata);
    try {
      cosmosStore.createItem(dpsHeaders.getPartitionId(), cosmosConfig.getDatabase(),
          cosmosConfig.getWorkflowMetadataCollection(), workflowMetadataDoc.getPartitionKey(),
          workflowMetadataDoc);
    } catch (AppException e) {
      String workflowName = workflowMetadataDoc.getWorkflowName();
      if(e.getError().getCode() == 409) {
        final String errorMessage = String.format("Workflow with name %s already exists", workflowName);
        logger.error(LOGGER_NAME, errorMessage);
        throw new ResourceConflictException(workflowName, errorMessage);
      } else {
        throw e;
      }
    }
    return buildWorkflowMetadata(workflowMetadataDoc);
  }

  @Override
  public WorkflowMetadata getWorkflow(final String workflowName) {
    String cacheKey = String.format("%s-%s", dpsHeaders.getPartitionId(), workflowName);
    WorkflowMetadata workflowMetadata = workflowMetadataCache.get(cacheKey);
    if (workflowMetadata != null) {
      return workflowMetadata;
    }
    Optional<WorkflowMetadataDoc> workflowMetadataDoc =
      cosmosStore.findItem(
        dpsHeaders.getPartitionId(),
        cosmosConfig.getDatabase(),
        cosmosConfig.getWorkflowMetadataCollection(),
        workflowName,
        workflowName,
        WorkflowMetadataDoc.class
      );
    if (null == workflowMetadataDoc || !workflowMetadataDoc.isPresent()) {
      final String errorMessage = String.format("Workflow: %s doesn't exist", workflowName);
      logger.error(LOGGER_NAME, errorMessage);
      throw new WorkflowNotFoundException(errorMessage);
    }
    workflowMetadata = buildWorkflowMetadata(workflowMetadataDoc.get());
    workflowMetadataCache.put(cacheKey, workflowMetadata);
    return workflowMetadata;
  }

  @Override
  public void deleteWorkflow(String workflowName) {
      String cacheKey = String.format("%s-%s", dpsHeaders.getPartitionId(), workflowName);
      workflowMetadataCache.delete(cacheKey);
      cosmosStore.deleteItem(
          dpsHeaders.getPartitionId(),
          cosmosConfig.getDatabase(),
          cosmosConfig.getWorkflowMetadataCollection(),
          workflowName,
          workflowName
      );
      // making sure to delete it from the cache in case there are scenarios where the key gets added
      // back to the workflow metadata cache when a GET request is called while deleting the item from cosmos
      workflowMetadataCache.delete(cacheKey);
  }

  @Override
  public List<WorkflowMetadata> getAllWorkflowForTenant(String prefix) {
    try {
      SqlQuerySpec sqlQuerySpec = buildSqlQuerySpecForGetAllWorkflow(prefix);
      final List<WorkflowMetadataDoc> workflowMetadataDocs = cosmosStore.queryItems(
              dpsHeaders.getPartitionId(),
              cosmosConfig.getDatabase(),
              cosmosConfig.getWorkflowMetadataCollection(),
              sqlQuerySpec,
              new CosmosQueryRequestOptions(),
              WorkflowMetadataDoc.class);
      return convertWorkflowMetadataDocsToWorkflowMetadataList(workflowMetadataDocs);
    } catch (CosmosException e) {
      throw new AppException(e.getStatusCode(), e.getMessage(), e.getMessage(), e);
    }
  }
}
