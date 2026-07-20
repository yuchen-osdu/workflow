package org.opengroup.osdu.workflow.provider.azure.repository;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.SqlQuerySpec;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.provider.azure.config.CosmosConfig;
import org.opengroup.osdu.workflow.provider.azure.exception.CustomOperatorNotFoundException;
import org.opengroup.osdu.workflow.provider.azure.interfaces.ICustomOperatorMetadataRepository;
import org.opengroup.osdu.workflow.provider.azure.model.CustomOperatorDoc;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class CustomOperatorMetadataRepository implements ICustomOperatorMetadataRepository {
  private static final String LOGGER_NAME = CustomOperatorMetadataRepository.class.getName();

  @Autowired
  private CosmosConfig cosmosConfig;

  @Autowired
  private CosmosStore cosmosStore;

  @Autowired
  private DpsHeaders dpsHeaders;

  @Autowired
  private JaxRsDpsLog logger;

  @Override
  public CustomOperator saveMetadata(CustomOperator customOperator) {
    final CustomOperatorDoc customOperatorDoc = buildCustomOperatorDoc(customOperator);
    try {
      cosmosStore.createItem(dpsHeaders.getPartitionId(), cosmosConfig.getDatabase(),
          cosmosConfig.getCustomOperatorCollection(), customOperatorDoc.getPartitionKey(),
          customOperatorDoc);
      return buildCustomOperator(customOperatorDoc);
    } catch (AppException e) {
      if(e.getError().getCode() == 409) {
        final String errorMessage = String.format(
            "Custom operator with name %s and id %s already exists", customOperatorDoc.getName(),
            customOperatorDoc.getName());
        logger.error(errorMessage, e);
        throw new ResourceConflictException(customOperatorDoc.getName(), errorMessage);
      } else {
        throw e;
      }
    }
  }

  @Override
  public CustomOperator getMetadataByCustomOperatorName(String operatorName) {
    final Optional<CustomOperatorDoc> customOperatorDoc =
        cosmosStore.findItem(dpsHeaders.getPartitionId(),
            cosmosConfig.getDatabase(),
            cosmosConfig.getCustomOperatorCollection(),
            operatorName,
            operatorName,
            CustomOperatorDoc.class);
    if (customOperatorDoc.isPresent()) {
      return buildCustomOperator(customOperatorDoc.get());
    } else {
      final String errorMessage = String.format("Custom operator with id %s not found", operatorName);
      logger.error(LOGGER_NAME, errorMessage);
      throw new CustomOperatorNotFoundException(errorMessage);
    }
  }

  @Override
  public CustomOperatorsPage getMetadataForAllCustomOperators(Integer limit, String cursor) {
    if(cursor != null) {
      cursor = new String(Base64.getUrlDecoder().decode(cursor));
    }

    try {
      SqlQuerySpec sqlQuerySpec = new SqlQuerySpec("SELECT * FROM c ORDER BY c._ts DESC");
      final Page<CustomOperatorDoc> pagedCustomOperatorDoc =
          cosmosStore.queryItemsPage(dpsHeaders.getPartitionId(), cosmosConfig.getDatabase(),
              cosmosConfig.getCustomOperatorCollection(), sqlQuerySpec, CustomOperatorDoc.class,
              limit, cursor);
      return buildCustomOperatorsPage(pagedCustomOperatorDoc);
    } catch (CosmosException e) {
      throw new AppException(e.getStatusCode(), e.getMessage(), e.getMessage(), e);
    }
  }

  private CustomOperatorDoc buildCustomOperatorDoc(final CustomOperator customOperator) {
    return CustomOperatorDoc.builder()
        .id(customOperator.getName())
        .partitionKey(customOperator.getName())
        .name(customOperator.getName())
        .className(customOperator.getClassName())
        .description(customOperator.getDescription())
        .createdAt(customOperator.getCreatedAt())
        .createdBy(customOperator.getCreatedBy())
        .properties(customOperator.getProperties())
        .build();
  }

  private CustomOperator buildCustomOperator(final CustomOperatorDoc customOperatorDoc) {
    return CustomOperator.builder().id(customOperatorDoc.getId())
        .name(customOperatorDoc.getName())
        .className(customOperatorDoc.getClassName()).description(customOperatorDoc.getDescription())
        .createdAt(customOperatorDoc.getCreatedAt()).createdBy(customOperatorDoc.getCreatedBy())
        .properties(customOperatorDoc.getProperties()).build();
  }

  private CustomOperatorsPage buildCustomOperatorsPage(
      final Page<CustomOperatorDoc> pagedCustomOperatorDoc) {
    CosmosStorePageRequest cosmosPageRequest =
        (CosmosStorePageRequest) pagedCustomOperatorDoc.getPageable();
    List<CustomOperator> customOperators = new ArrayList<>();
    for(CustomOperatorDoc customOperatorDoc: pagedCustomOperatorDoc.getContent()) {
      customOperators.add(buildCustomOperator(customOperatorDoc));
    }

    CustomOperatorsPage.CustomOperatorsPageBuilder customOperatorsPageBuilder =
        CustomOperatorsPage.builder().items(customOperators);
    if(cosmosPageRequest.getRequestContinuation() != null) {
      customOperatorsPageBuilder.cursor(Base64.getUrlEncoder()
          .encodeToString(cosmosPageRequest.getRequestContinuation().getBytes()));
    }
    return customOperatorsPageBuilder.build();
  }
}
