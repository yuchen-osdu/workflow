package org.opengroup.osdu.workflow.provider.azure.repository;

import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.azure.query.CosmosStorePageRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.provider.azure.config.CosmosConfig;
import org.opengroup.osdu.workflow.provider.azure.exception.CustomOperatorNotFoundException;
import org.opengroup.osdu.workflow.provider.azure.model.CustomOperatorDoc;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorProperty;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CustomOperatorMetadataRepository}
 */
@ExtendWith(MockitoExtension.class)
public class CustomOperatorMetadataRepositoryTest {
  private static final String PARTITION_ID = "someId";
  private static final String DATABASE_NAME = "someDbName";
  private static final String CUSTOM_OPERATOR_COLLECTION = "someCollection";
  private static final String INPUT_CUSTOM_OPERATOR = "{\n" +
      "    \"name\": \"foo_operator\",\n" +
      "    \"className\": \"HelloWorld\",\n" +
      "    \"description\": \"Used to print hello world\",\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"createdAt\": \"123456\",\n" +
      "    \"properties\": [\n" +
      "        {\n" +
      "            \"name\": \"some Variable\",\n" +
      "            \"description\": \"some Description\",\n" +
      "            \"mandatory\": true\n" +
      "        }\n" +
      "    ]\n" +
      "}";
  private static final String OUTPUT_CUSTOM_OPERATOR = "{\n" +
      "\t\"id\": \"foo_operator\",\n" +
      "    \"name\": \"foo_operator\",\n" +
      "    \"className\": \"HelloWorld\",\n" +
      "    \"description\": \"Used to print hello world\",\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"createdAt\": \"123456\",\n" +
      "    \"properties\": [\n" +
      "        {\n" +
      "            \"name\": \"some Variable\",\n" +
      "            \"description\": \"some Description\",\n" +
      "            \"mandatory\": true\n" +
      "        }\n" +
      "    ]\n" +
      "}";
  private static final String OUTPUT_CUSTOM_OPERATOR_DOC = "{\n" +
      "    \"id\": \"foo_operator\",\n" +
      "    \"partitionKey\": \"foo_operator\",\n" +
      "    \"name\": \"foo_operator\",\n" +
      "    \"className\": \"HelloWorld\",\n" +
      "    \"description\": \"Used to print hello world\",\n" +
      "    \"createdBy\": \"user@email.com\",\n" +
      "    \"createdAt\": \"123456\",\n" +
      "    \"properties\": [\n" +
      "        {\n" +
      "            \"name\": \"some Variable\",\n" +
      "            \"description\": \"some Description\",\n" +
      "            \"mandatory\": true\n" +
      "        }\n" +
      "    ]\n" +
      "}";
  private static final String VALID_ID = "foo_operator";
  private static final String VALID_PARTITION_KEY = "foo_operator";
  private static final String INVALID_ID = "invalid_operator";
  private static final String INVALID_PARTITION_KEY = "invalid_operator";
  private static final String REQUEST_CONTINUATION =
      "{\"token\":\"-RID:~O0wNANEhDQECAAAAAAAAAA==#RT:1#TRC:1#ISV:2#IEO:65551\",\n" +
      "\"range\":\"{\\\"min\\\":\\\"\\\",\\\"max\\\":\\\"FF\\\",\\\"isMinInclusive\\\":true,\n" +
      "\\\"isMaxInclusive\\\":false}\"}";
  private static final String CURSOR_VALUE = "eyJ0b2tlbiI6Ii1SSUQ6fk8wd05BTkVoRFFFQ0FBQUFBQUFBQUE9P" +
      "SNSVDoxI1RSQzoxI0lTVjoyI0lFTzo2NTU1MSIsCiJyYW5nZSI6IntcIm1pblwiOlwiXCIsXCJtYXhcIjpcIkZGXCIsX" +
      "CJpc01pbkluY2x1c2l2ZVwiOnRydWUsClwiaXNNYXhJbmNsdXNpdmVcIjpmYWxzZX0ifQ==";


  @Mock
  private CosmosConfig cosmosConfig;

  @Mock
  private CosmosStore cosmosStore;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private JaxRsDpsLog jaxRsDpsLog;

  @InjectMocks
  private CustomOperatorMetadataRepository customOperatorMetadataRepository;

  private final ObjectMapper objectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void testSaveMetadataWithValidCustomOperator() throws Exception {
    CustomOperator customOperator = objectMapper.readValue(INPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doNothing().when(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(CUSTOM_OPERATOR_COLLECTION),
            eq(VALID_PARTITION_KEY), any(CustomOperatorDoc.class));

    final CustomOperator createdCustomOperator = customOperatorMetadataRepository
        .saveMetadata(customOperator);

    CustomOperator expectedCustomOperator = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    verifyCustomOperator(createdCustomOperator, expectedCustomOperator);
    verify(cosmosStore, times(1)).createItem(eq(PARTITION_ID),
        eq(DATABASE_NAME), eq(CUSTOM_OPERATOR_COLLECTION),
        eq(VALID_PARTITION_KEY), any(CustomOperatorDoc.class));

  }

  @Test
  public void testSaveMetadataWithExistingCustomOperator() throws Exception {
    CustomOperator customOperator = objectMapper.readValue(INPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doThrow(new AppException(409, "", "")).when(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(CUSTOM_OPERATOR_COLLECTION),
            eq(VALID_PARTITION_KEY), any(CustomOperatorDoc.class));

    Assertions.assertThrows(ResourceConflictException.class, () -> {
      customOperatorMetadataRepository.saveMetadata(customOperator);
    });

    verify(cosmosStore, times(1)).createItem(eq(PARTITION_ID),
        eq(DATABASE_NAME), eq(CUSTOM_OPERATOR_COLLECTION),
        eq(VALID_PARTITION_KEY), any(CustomOperatorDoc.class));
  }

  @Test
  public void testSaveMetadataIfInternalError() throws Exception {
    CustomOperator customOperator = objectMapper.readValue(INPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    doThrow(new AppException(500, "", "")).when(cosmosStore)
        .createItem(eq(PARTITION_ID), eq(DATABASE_NAME), eq(CUSTOM_OPERATOR_COLLECTION),
            eq(VALID_PARTITION_KEY), any(CustomOperatorDoc.class));

    boolean isExceptionThrown = false;
    try {
      customOperatorMetadataRepository.saveMetadata(customOperator);
    } catch (AppException e) {
      isExceptionThrown = true;
    }

    verify(cosmosStore, times(1)).createItem(eq(PARTITION_ID),
        eq(DATABASE_NAME), eq(CUSTOM_OPERATOR_COLLECTION),
        eq(VALID_PARTITION_KEY), any(CustomOperatorDoc.class));
    assertThat(isExceptionThrown, equalTo(true));
  }

  @Test
  public void testGetMetadataWithCustomerIdWithExistingId() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    CustomOperatorDoc customOperator = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR_DOC,
        CustomOperatorDoc.class);
    when(cosmosStore.findItem(PARTITION_ID, DATABASE_NAME, CUSTOM_OPERATOR_COLLECTION, VALID_ID,
        VALID_PARTITION_KEY, CustomOperatorDoc.class)).thenReturn(Optional.of(customOperator));

    CustomOperator returnedCustomOperator = customOperatorMetadataRepository
        .getMetadataByCustomOperatorName(VALID_ID);
    CustomOperator expectedCustomOperator = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    verifyCustomOperator(returnedCustomOperator, expectedCustomOperator);
    verify(cosmosStore, times(1)).findItem(PARTITION_ID, DATABASE_NAME,
        CUSTOM_OPERATOR_COLLECTION, VALID_ID, VALID_PARTITION_KEY, CustomOperatorDoc.class);
  }

  @Test
  public void testGetMetadataWithCustomerIdWithNonExistingId() {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    when(cosmosStore.findItem(PARTITION_ID, DATABASE_NAME, CUSTOM_OPERATOR_COLLECTION, INVALID_ID,
        INVALID_PARTITION_KEY, CustomOperatorDoc.class)).thenReturn(Optional.empty());

    boolean isException = false;
    try {
      CustomOperator returnedCustomOperator = customOperatorMetadataRepository
          .getMetadataByCustomOperatorName(INVALID_ID);
    } catch (CustomOperatorNotFoundException e) {
      isException = true;
    }

    verify(cosmosStore, times(1)).findItem(PARTITION_ID, DATABASE_NAME,
        CUSTOM_OPERATOR_COLLECTION, INVALID_ID, INVALID_PARTITION_KEY, CustomOperatorDoc.class);
    assertThat(isException, equalTo(true));
  }

  @Test
  public void testGetMetadataForAllCustomOperatorsIfCursorNull() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    CustomOperatorDoc customOperatorDoc = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR_DOC,
        CustomOperatorDoc.class);
    Page<CustomOperatorDoc> customOperatorDocPage = new PageImpl<>(Arrays.asList(customOperatorDoc),
        CosmosStorePageRequest.of(1,1, REQUEST_CONTINUATION), 1);
    when(cosmosStore.queryItemsPage(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(CUSTOM_OPERATOR_COLLECTION), any(SqlQuerySpec.class), eq(CustomOperatorDoc.class),
        eq(1), eq(null))).thenReturn(customOperatorDocPage);

    CustomOperatorsPage customOperatorsPage = customOperatorMetadataRepository
        .getMetadataForAllCustomOperators(1, null);
    verify(cosmosStore, times(1)).queryItemsPage(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(CUSTOM_OPERATOR_COLLECTION), any(SqlQuerySpec.class), eq(CustomOperatorDoc.class),
        eq(1), eq(null));
    assertThat(customOperatorsPage.getCursor(), equalTo(CURSOR_VALUE));
    List<CustomOperator> customOperators = customOperatorsPage.getItems();
    assertThat(customOperators.size(), equalTo(1));
    CustomOperator expectedCustomOperator = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    verifyCustomOperator(customOperators.get(0), expectedCustomOperator);
  }

  @Test
  public void testGetMetadataForAllCustomOperatorsIfCursorNonNull() throws Exception {
    when(cosmosConfig.getDatabase()).thenReturn(DATABASE_NAME);
    when(cosmosConfig.getCustomOperatorCollection()).thenReturn(CUSTOM_OPERATOR_COLLECTION);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION_ID);
    CustomOperatorDoc customOperatorDoc = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR_DOC,
        CustomOperatorDoc.class);
    Page<CustomOperatorDoc> customOperatorDocPage = new PageImpl<>(Arrays.asList(customOperatorDoc),
        CosmosStorePageRequest.of(1,1, null, Sort.unsorted()), 1);
    when(cosmosStore.queryItemsPage(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(CUSTOM_OPERATOR_COLLECTION), any(SqlQuerySpec.class), eq(CustomOperatorDoc.class),
        eq(1), eq(REQUEST_CONTINUATION))).thenReturn(customOperatorDocPage);

    CustomOperatorsPage customOperatorsPage = customOperatorMetadataRepository
        .getMetadataForAllCustomOperators(1, CURSOR_VALUE);
    verify(cosmosStore, times(1)).queryItemsPage(eq(PARTITION_ID), eq(DATABASE_NAME),
        eq(CUSTOM_OPERATOR_COLLECTION), any(SqlQuerySpec.class), eq(CustomOperatorDoc.class),
        eq(1), eq(REQUEST_CONTINUATION));
    assertThat(customOperatorsPage.getCursor(), equalTo(null));
    List<CustomOperator> customOperators = customOperatorsPage.getItems();
    assertThat(customOperators.size(), equalTo(1));
    CustomOperator expectedCustomOperator = objectMapper.readValue(OUTPUT_CUSTOM_OPERATOR,
        CustomOperator.class);
    verifyCustomOperator(customOperators.get(0), expectedCustomOperator);
  }

  private void verifyCustomOperator(CustomOperator customOperator,
                                    CustomOperator expectedCustomOperator) {
    assertThat(customOperator.getId(), equalTo(expectedCustomOperator.getId()));
    assertThat(customOperator.getDescription(),
        equalTo(expectedCustomOperator.getDescription()));
    assertThat(customOperator.getCreatedBy(),
        equalTo(expectedCustomOperator.getCreatedBy()));
    assertThat(customOperator.getCreatedAt(),
        equalTo(expectedCustomOperator.getCreatedAt()));
    assertThat(customOperator.getProperties() != null,
        equalTo(expectedCustomOperator.getProperties() != null));
    if(customOperator.getProperties() != null) {
      assertThat(customOperator.getProperties().size(),
          equalTo(expectedCustomOperator.getProperties().size()));
      for(int i = 0; i < expectedCustomOperator.getProperties().size(); i++) {
        CustomOperatorProperty createdCustomOperatorProperty = customOperator
            .getProperties().get(i);
        CustomOperatorProperty expectedCustomOperatorProperty =
            expectedCustomOperator.getProperties().get(i);
        assertThat(createdCustomOperatorProperty.getName(),
            equalTo(expectedCustomOperatorProperty.getName()));
        assertThat(createdCustomOperatorProperty.getDescription(),
            equalTo(expectedCustomOperatorProperty.getDescription()));
        assertThat(createdCustomOperatorProperty.getMandatory(),
            equalTo(expectedCustomOperatorProperty.getMandatory()));
      }
    }
  }
 }
