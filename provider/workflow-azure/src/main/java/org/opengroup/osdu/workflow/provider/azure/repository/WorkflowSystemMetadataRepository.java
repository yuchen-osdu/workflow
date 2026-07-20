package org.opengroup.osdu.workflow.provider.azure.repository;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlQuerySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.azure.config.AzureWorkflowEngineConfig;
import org.opengroup.osdu.workflow.provider.azure.config.CosmosConfig;
import org.opengroup.osdu.workflow.provider.azure.model.WorkflowMetadataDoc;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowSystemMetadataRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.opengroup.osdu.workflow.provider.azure.utils.WorkflowMetadataUtils.buildSqlQuerySpecForGetAllWorkflow;
import static org.opengroup.osdu.workflow.provider.azure.utils.WorkflowMetadataUtils.buildWorkflowMetadata;
import static org.opengroup.osdu.workflow.provider.azure.utils.WorkflowMetadataUtils.buildWorkflowMetadataDoc;
import static org.opengroup.osdu.workflow.provider.azure.utils.WorkflowMetadataUtils.convertWorkflowMetadataDocsToWorkflowMetadataList;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WorkflowSystemMetadataRepository implements IWorkflowSystemMetadataRepository {
  private static final String LOGGER_NAME = WorkflowMetadataRepository.class.getName();

  private final CosmosConfig cosmosConfig;

  private final CosmosStore cosmosStore;

  private final JaxRsDpsLog logger;

  private final AzureWorkflowEngineConfig workflowEngineConfig;

  @Qualifier("WorkflowMetadataCache")
  private final ICache<String, WorkflowMetadata> workflowMetadataCache;

  @Override
  public WorkflowMetadata getSystemWorkflow(final String workflowName) {
    WorkflowMetadata workflowMetadata = workflowMetadataCache.get(workflowName);
    if (workflowMetadata != null) {
      return workflowMetadata;
    }
    Optional<WorkflowMetadataDoc> workflowSystemMetadataDoc =
        cosmosStore.findItem(
          cosmosConfig.getSystemdatabase(),
          cosmosConfig.getWorkflowMetadataCollection(),
          workflowName,
          workflowName,
          WorkflowMetadataDoc.class
        );
    if (null == workflowSystemMetadataDoc || !workflowSystemMetadataDoc.isPresent()) {
      final String errorMessage = String.format("Workflow: %s doesn't exist", workflowName);
      logger.error(LOGGER_NAME, errorMessage);
      throw new WorkflowNotFoundException(errorMessage);
    }
    workflowMetadata = buildWorkflowMetadata(workflowSystemMetadataDoc.get());
    workflowMetadataCache.put(workflowName, workflowMetadata);
    return workflowMetadata;
  }

  @Override
  public WorkflowMetadata createSystemWorkflow(final WorkflowMetadata workflowMetadata) {
    final WorkflowMetadataDoc workflowMetadataDoc = buildWorkflowMetadataDoc(workflowEngineConfig, workflowMetadata);
    try {
      cosmosStore.createItem(
        cosmosConfig.getSystemdatabase(),
        cosmosConfig.getWorkflowMetadataCollection(),
        workflowMetadataDoc.getPartitionKey(),
        workflowMetadataDoc
      );
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
  public void deleteSystemWorkflow(String workflowName) {
      workflowMetadataCache.delete(workflowName);
      cosmosStore.deleteItem(
        cosmosConfig.getSystemdatabase(),
        cosmosConfig.getWorkflowMetadataCollection(),
        workflowName,
        workflowName
      );
    // making sure to delete it from the cache in case there are scenarios where the key gets added
    // back to the workflow metadata cache when a GET request is called while deleting the item from cosmos
    workflowMetadataCache.delete(workflowName);
  }

  @Override
  public List<WorkflowMetadata> getAllSystemWorkflow(String prefix) {
    SqlQuerySpec sqlQuerySpec = buildSqlQuerySpecForGetAllWorkflow(prefix);
    final List<WorkflowMetadataDoc> workflowSystemMetadataDocs = cosmosStore.queryItems(
        cosmosConfig.getSystemdatabase(),
        cosmosConfig.getWorkflowMetadataCollection(),
        sqlQuerySpec,
        new CosmosQueryRequestOptions(),
        WorkflowMetadataDoc.class);
    return convertWorkflowMetadataDocsToWorkflowMetadataList(workflowSystemMetadataDocs);
  }
}
