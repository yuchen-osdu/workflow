package org.opengroup.osdu.workflow.provider.azure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.provider.azure.config.AzureWorkflowEngineConfig;
import org.opengroup.osdu.workflow.provider.azure.exception.CustomOperatorNotFoundException;
import org.opengroup.osdu.workflow.provider.azure.interfaces.ICustomOperatorMetadataRepository;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.RegisterCustomOperatorRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CustomOperatorServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
public class CustomOperatorServiceTest {
  private static final String CUSTOM_OPERATOR_WITH_CONTENT = "{\n" +
      "    \"name\": \"hello_world_operator\",\n" +
      "    \"className\": \"HelloWorld\",\n" +
      "    \"description\": \"Used to print hello world\",\n" +
      "    \"content\": \"Content of the custom operator\",\n" +
      "    \"properties\": [\n" +
      "        {\n" +
      "            \"name\": \"some Variable\",\n" +
      "            \"description\": \"some Description\",\n" +
      "            \"mandatory\": true\n" +
      "        }\n" +
      "    ]\n" +
      "}";
  private static final String USER_EMAIL = "user@email.com";
  private static final String VALID_OPERATOR_ID = "12345";
  private static final String INVALID_OPERATOR_ID = "67890";
  private static final String EXISTING_OPERATOR_ID = "existing-id";
  private static final List<Integer> VALID_LIMITS = Arrays.asList(1, 10, 50);
  private static final List<Integer> INVALID_LIMITS = Arrays.asList(-1, 0, 60);

  @Mock
  private ICustomOperatorMetadataRepository customOperatorMetadataRepository;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private IWorkflowEngineService workflowEngineService;

  @Mock
  private AzureWorkflowEngineConfig workflowEngineConfig;

  @InjectMocks
  private CustomOperatorServiceImpl customOperatorService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testRegisterNewOperatorWithNewOperator_whenIgnoreDagContentIsDisabled() throws Exception {
    RegisterCustomOperatorRequest newOperatorRequest = objectMapper.readValue(CUSTOM_OPERATOR_WITH_CONTENT,
        RegisterCustomOperatorRequest.class);
    final ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> contentArgumentCaptor = ArgumentCaptor.forClass(String.class);
    doNothing().when(workflowEngineService).saveCustomOperator(contentArgumentCaptor.capture(),
        fileNameArgumentCaptor.capture());
    final ArgumentCaptor<CustomOperator> customOperatorArgumentCaptor =
        ArgumentCaptor.forClass(CustomOperator.class);
    final CustomOperator responseCustomOperator = mock(CustomOperator.class);
    when(customOperatorMetadataRepository.saveMetadata(customOperatorArgumentCaptor.capture()))
        .thenReturn(responseCustomOperator);
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    when(workflowEngineConfig.getIgnoreCustomOperatorContent()).thenReturn(false);

    CustomOperator customOperator = customOperatorService.registerNewOperator(newOperatorRequest);

    verify(customOperatorMetadataRepository, times(1))
        .saveMetadata(ArgumentMatchers.any(CustomOperator.class));
    verify(workflowEngineService).saveCustomOperator(ArgumentMatchers.any(String.class),
            ArgumentMatchers.any(String.class));
    verify(workflowEngineConfig, times(1)).getIgnoreCustomOperatorContent();
    assertThat(customOperator, equalTo(responseCustomOperator));
    assertThat(contentArgumentCaptor.getValue(), equalTo(newOperatorRequest.getContent()));
    assertThat(fileNameArgumentCaptor.getValue(),
        equalTo(newOperatorRequest.getName() + ".py"));
    CustomOperator constructedCustomOperator = customOperatorArgumentCaptor.getValue();
    checkCustomOperatorWithOperatorRequest(constructedCustomOperator, newOperatorRequest);
  }

  @Test
  public void testRegisterNewOperatorWithNewOperator_throwsException_whenIgnoreDagContentIsEnabled() throws Exception {
    RegisterCustomOperatorRequest newOperatorRequest = objectMapper.readValue(CUSTOM_OPERATOR_WITH_CONTENT,
        RegisterCustomOperatorRequest.class);
    when(workflowEngineConfig.getIgnoreCustomOperatorContent()).thenReturn(true);

    Assertions.assertThrows(AppException.class, () -> {
      customOperatorService.registerNewOperator(newOperatorRequest);
    });

    verify(workflowEngineConfig, times(1)).getIgnoreCustomOperatorContent();
  }

  @Test
  public void testRegisterNewOperatorWithExistingOperator() throws Exception {
    RegisterCustomOperatorRequest newOperatorRequest = objectMapper.readValue(CUSTOM_OPERATOR_WITH_CONTENT,
        RegisterCustomOperatorRequest.class);
    final ArgumentCaptor<CustomOperator> customOperatorArgumentCaptor =
        ArgumentCaptor.forClass(CustomOperator.class);
    doThrow(new ResourceConflictException(EXISTING_OPERATOR_ID,  "operator exists"))
        .when(customOperatorMetadataRepository).saveMetadata(
            customOperatorArgumentCaptor.capture());
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    Assertions.assertThrows(ResourceConflictException.class, () -> {
      customOperatorService.registerNewOperator(newOperatorRequest);
    });

    verify(customOperatorMetadataRepository, times(1))
        .saveMetadata(ArgumentMatchers.any(CustomOperator.class));
    verify(workflowEngineService, times(0))
        .saveCustomOperator(ArgumentMatchers.any(String.class),
            ArgumentMatchers.any(String.class));
    CustomOperator constructedCustomOperator = customOperatorArgumentCaptor.getValue();
    checkCustomOperatorWithOperatorRequest(constructedCustomOperator, newOperatorRequest);
  }

  @Test
  public void testRegisterNewOperatorThrowsExceptionIfUnknownError() throws Exception {
    RegisterCustomOperatorRequest newOperatorRequest = objectMapper.readValue(CUSTOM_OPERATOR_WITH_CONTENT,
        RegisterCustomOperatorRequest.class);
    final ArgumentCaptor<CustomOperator> customOperatorArgumentCaptor =
        ArgumentCaptor.forClass(CustomOperator.class);
    when(customOperatorMetadataRepository.saveMetadata(customOperatorArgumentCaptor.capture()))
        .thenThrow(new AppException(500, "", ""));
    when(dpsHeaders.getUserEmail()).thenReturn(USER_EMAIL);
    Assertions.assertThrows(AppException.class, () -> {
      customOperatorService.registerNewOperator(newOperatorRequest);
    });
    verify(customOperatorMetadataRepository, times(1))
        .saveMetadata(ArgumentMatchers.any(CustomOperator.class));
    verify(workflowEngineService, times(0))
        .saveCustomOperator(ArgumentMatchers.any(String.class),
            ArgumentMatchers.any(String.class));
    CustomOperator constructedCustomOperator = customOperatorArgumentCaptor.getValue();
    checkCustomOperatorWithOperatorRequest(constructedCustomOperator, newOperatorRequest);
  }

  private void checkCustomOperatorWithOperatorRequest(
      CustomOperator constructedCustomOperator, RegisterCustomOperatorRequest newOperatorRequest) {
    assertThat(constructedCustomOperator.getName(), equalTo(newOperatorRequest.getName()));
    assertThat(constructedCustomOperator.getClassName(),
        equalTo(newOperatorRequest.getClassName()));
    assertThat(constructedCustomOperator.getCreatedBy(), equalTo(USER_EMAIL));
    assertThat(constructedCustomOperator.getDescription(),
        equalTo(newOperatorRequest.getDescription()));
    assertThat(constructedCustomOperator.getProperties(),
        equalTo(newOperatorRequest.getProperties()));
    assertThat(constructedCustomOperator.getId(), equalTo(null));
  }

  @Test
  public void testGetOperatorByIdWithValidId() {
    final CustomOperator mockedCustomOperator = mock(CustomOperator.class);
    when(customOperatorMetadataRepository.getMetadataByCustomOperatorName(VALID_OPERATOR_ID))
        .thenReturn(mockedCustomOperator);

    CustomOperator customOperator = customOperatorService.getOperatorByName(VALID_OPERATOR_ID);

    verify(customOperatorMetadataRepository, times(1))
        .getMetadataByCustomOperatorName(VALID_OPERATOR_ID);
    assertThat(customOperator, equalTo(mockedCustomOperator));
  }

  @Test
  public void testGetOperatorByIdWithInvalidId() {
    when(customOperatorMetadataRepository.getMetadataByCustomOperatorName(INVALID_OPERATOR_ID))
        .thenThrow(new CustomOperatorNotFoundException("Not found"));
    Assertions.assertThrows(CustomOperatorNotFoundException.class, () -> {
      customOperatorService.getOperatorByName(INVALID_OPERATOR_ID);
    });
  }

  @Test
  public void testGetAllOperatorsWithValidLimit() {
    final CustomOperatorsPage mockedCustomerOperatorPage = mock(CustomOperatorsPage.class);
    when(customOperatorMetadataRepository
        .getMetadataForAllCustomOperators(anyInt(), anyString()))
        .thenReturn(mockedCustomerOperatorPage);

    List<CustomOperatorsPage> outputCustomOperatorsPages = new ArrayList<>();
    for(Integer validLimit: VALID_LIMITS) {
      outputCustomOperatorsPages.add(customOperatorService.getAllOperators(validLimit, ""));
    }

    verify(customOperatorMetadataRepository, times(VALID_LIMITS.size()))
        .getMetadataForAllCustomOperators(anyInt(), anyString());
    for(CustomOperatorsPage outputCustomOperatorsPage: outputCustomOperatorsPages) {
      assertThat(outputCustomOperatorsPage, equalTo(mockedCustomerOperatorPage));
    }

  }

  @Test
  public void testGetAllOperatorsWithInvalidLimit() {
    int noOfTimesExceptionThrown = 0;
    for(Integer invalidLimit: INVALID_LIMITS) {
      Assertions.assertThrows(BadRequestException.class, () -> {
        customOperatorService.getAllOperators(invalidLimit, "");
      });
    }

    verify(customOperatorMetadataRepository, times(0))
        .getMetadataForAllCustomOperators(anyInt(), any());  }
}
