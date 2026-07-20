package org.opengroup.osdu.workflow.provider.azure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.handler.ConflictApiError;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.provider.azure.api.CustomOperatorApi;
import org.opengroup.osdu.workflow.provider.azure.exception.CustomOperatorNotFoundException;
import org.opengroup.osdu.workflow.provider.azure.interfaces.ICustomOperatorService;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IWorkflowTasksSharingService;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.RegisterCustomOperatorRequest;
import org.opengroup.osdu.workflow.security.AuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.context.annotation.Bean;

import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link CustomOperatorApi}
 */
@WebMvcTest(CustomOperatorApi.class)
@AutoConfigureMockMvc
@Import({AuthorizationFilter.class, DpsHeaders.class})
@Disabled
public class CustomOperatorMvcTest {
  private static final String TEST_AUTH = "Bearer bla";
  private static final String PARTITION = "partition";
  private static final String CUSTOM_OPERATOR_ENDPOINT = "/customOperator";
  private static final String CUSTOM_OPERATOR_BY_ID_ENDPOINT = "/customOperator/{customOperatorId}";
  private static final String EXISTING_CUSTOM_OPERATOR_ID = "existing-id";
  private static final String REGISTER_OPERATOR_REQUEST = "{\n" +
      "    \"name\": \"foo_operator\",\n" +
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
  private static final String RESPONSE_CUSTOM_OPERATOR = "{\n" +
      "\t\"id\": \"Zm9vX29wZXJhdG9y\",\n" +
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
  private static final String RESPONSE_CUSTOM_OPERATORS_PAGE = "{\n" +
      "    \"items\": [\n" +
      "        {\n" +
      "            \"id\": \"aGVsbG9fd29ybGRfZm9vXzFfb3BlcmF0b3I=\",\n" +
      "            \"name\": \"hello_world_foo_1_operator\",\n" +
      "            \"className\": \"HelloWorld\",\n" +
      "            \"description\": \"Used to print hello world\",\n" +
      "            \"createdAt\": 1605861652882,\n" +
      "            \"properties\": [\n" +
      "                {\n" +
      "                    \"name\": \"some Variable\",\n" +
      "                    \"description\": \"some Description\",\n" +
      "                    \"mandatory\": true\n" +
      "                }\n" +
      "            ]\n" +
      "        }\n" +
      "    ],\n" +
      "    \"cursor\": \"foobar\"\n" +
      "}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private ICustomOperatorService customOperatorService;

  @MockBean
  private IWorkflowTasksSharingService workflowTasksSharingService;

  @MockBean
  private IAuthorizationService authorizationService;

  @MockBean
  private JaxRsDpsLog log;

  @Mock
  private AuthorizationResponse authorizationResponse;

  @Test
  public void testRegisterCustomOperatorWithSuccess() throws Exception {
    final RegisterCustomOperatorRequest request = mapper.readValue(REGISTER_OPERATOR_REQUEST,
        RegisterCustomOperatorRequest.class);
    final CustomOperator customOperator = mapper.readValue(RESPONSE_CUSTOM_OPERATOR,
        CustomOperator.class);
    when(customOperatorService.registerNewOperator(eq(request))).thenReturn(customOperator);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN)))
        .thenReturn(authorizationResponse);

    final MvcResult mvcResult = mockMvc.perform(
            post(CUSTOM_OPERATOR_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(getHttpHeaders())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(REGISTER_OPERATOR_REQUEST))
        .andExpect(status().isOk())
        .andReturn();
    verify(customOperatorService, times(1)).registerNewOperator(eq(request));
    verify(authorizationService, times(1)).authorizeAny(any(),
        eq(WorkflowRole.ADMIN));
    final CustomOperator response = mapper.readValue(mvcResult.getResponse().getContentAsByteArray(),
        CustomOperator.class);
    assertThat(customOperator, equalTo(response));
  }

  @Test
  public void testRegisterCustomOperatorWithConflict() throws Exception {
    final RegisterCustomOperatorRequest request = mapper.readValue(REGISTER_OPERATOR_REQUEST,
        RegisterCustomOperatorRequest.class);
    when(customOperatorService.registerNewOperator(eq(request)))
        .thenThrow(new ResourceConflictException(EXISTING_CUSTOM_OPERATOR_ID, "Operator exists"));
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN)))
        .thenReturn(authorizationResponse);

    final MvcResult mvcResult = mockMvc.perform(
            post(CUSTOM_OPERATOR_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(getHttpHeaders())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(REGISTER_OPERATOR_REQUEST))
        .andExpect(status().isConflict())
        .andReturn();
    verify(customOperatorService, times(1)).registerNewOperator(eq(request));
    verify(authorizationService, times(1)).authorizeAny(any(),
        eq(WorkflowRole.ADMIN));
    final ConflictApiError response = mapper.readValue(mvcResult.getResponse()
        .getContentAsByteArray(), ConflictApiError.class);
    Assertions.assertEquals(EXISTING_CUSTOM_OPERATOR_ID, response.getConflictId());
  }

  @Test
  public void testGetAllCustomOperatorsWithoutQueryParams() throws Exception {
    final CustomOperatorsPage mockedCustomOperatorsPage = mapper.readValue(
        RESPONSE_CUSTOM_OPERATORS_PAGE, CustomOperatorsPage.class);
    when(customOperatorService.getAllOperators(50, null))
        .thenReturn(mockedCustomOperatorsPage);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN), eq(WorkflowRole.CREATOR),
        eq(WorkflowRole.VIEWER))).thenReturn(authorizationResponse);

    final MvcResult mvcResult = mockMvc.perform(
            get(CUSTOM_OPERATOR_ENDPOINT)
                .headers(getHttpHeaders())
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andReturn();
    verify(customOperatorService, times(1)).getAllOperators(50, null);
    verify(authorizationService, times(1)).authorizeAny(any(), eq(WorkflowRole.ADMIN),
        eq(WorkflowRole.CREATOR), eq(WorkflowRole.VIEWER));
    final CustomOperatorsPage response = mapper.readValue(mvcResult.getResponse()
        .getContentAsByteArray(), CustomOperatorsPage.class);
    assertThat(mockedCustomOperatorsPage, equalTo(response));
  }

  @Test
  public void testGetAllCustomOperatorsWithQueryParams() throws Exception {
    final Integer limit = 20;
    final String cursor = "sample_cursor";
    final CustomOperatorsPage mockedCustomOperatorsPage = mapper.readValue(
        RESPONSE_CUSTOM_OPERATORS_PAGE, CustomOperatorsPage.class);
    when(customOperatorService.getAllOperators(limit, cursor))
        .thenReturn(mockedCustomOperatorsPage);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN), eq(WorkflowRole.CREATOR),
        eq(WorkflowRole.VIEWER))).thenReturn(authorizationResponse);

    final MvcResult mvcResult = mockMvc.perform(
            get(CUSTOM_OPERATOR_ENDPOINT)
                .headers(getHttpHeaders())
                .queryParam("limit", Integer.toString(limit))
                .queryParam("cursor", cursor)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andReturn();
    verify(customOperatorService, times(1)).getAllOperators(limit, cursor);
    verify(authorizationService, times(1)).authorizeAny(any(),
        eq(WorkflowRole.ADMIN), eq(WorkflowRole.CREATOR), eq(WorkflowRole.VIEWER));
    final CustomOperatorsPage response = mapper.readValue(mvcResult.getResponse()
        .getContentAsByteArray(), CustomOperatorsPage.class);
    assertThat(mockedCustomOperatorsPage, equalTo(response));
  }

  @Test
  public void testGetCustomOperatorByIdWithSuccess() throws Exception {
    final String id = "Zm9vX29wZXJhdG9y";
    final CustomOperator customOperator = mapper.readValue(RESPONSE_CUSTOM_OPERATOR,
        CustomOperator.class);
    when(customOperatorService.getOperatorByName(id)).thenReturn(customOperator);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN), eq(WorkflowRole.CREATOR),
        eq(WorkflowRole.VIEWER))).thenReturn(authorizationResponse);

    final MvcResult mvcResult = mockMvc.perform(
            get(CUSTOM_OPERATOR_BY_ID_ENDPOINT, id)
                .headers(getHttpHeaders())
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andReturn();
    verify(customOperatorService, times(1)).getOperatorByName(id);
    verify(authorizationService, times(1)).authorizeAny(any(), eq(WorkflowRole.ADMIN),
        eq(WorkflowRole.CREATOR), eq(WorkflowRole.VIEWER));
    final CustomOperator response = mapper.readValue(mvcResult.getResponse()
        .getContentAsByteArray(), CustomOperator.class);
    assertThat(customOperator, equalTo(response));
  }

  @Test
  public void testGetCustomOperatorByIdWithInvalidId() throws Exception {
    final String invalidId = "wdqqfqfqweew";
    when(customOperatorService.getOperatorByName(invalidId))
        .thenThrow(new CustomOperatorNotFoundException("Not found"));
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN), eq(WorkflowRole.CREATOR),
        eq(WorkflowRole.VIEWER))).thenReturn(authorizationResponse);

    final MvcResult mvcResult = mockMvc.perform(
            get(CUSTOM_OPERATOR_BY_ID_ENDPOINT, invalidId)
                .headers(getHttpHeaders())
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isNotFound())
        .andReturn();
    verify(customOperatorService, times(1)).getOperatorByName(invalidId);
    verify(authorizationService, times(1)).authorizeAny(any(), eq(WorkflowRole.ADMIN),
        eq(WorkflowRole.CREATOR), eq(WorkflowRole.VIEWER));
  }

  private HttpHeaders getHttpHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(DpsHeaders.AUTHORIZATION, TEST_AUTH);
    headers.add(DpsHeaders.DATA_PARTITION_ID, PARTITION);
    return headers;
  }

  @TestConfiguration
  @EnableWebSecurity
  @EnableMethodSecurity
  public static class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http
          .cors(AbstractHttpConfigurer::disable)
          .csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
          .httpBasic(withDefaults());
      return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
      return (web) -> web.ignoring().requestMatchers("/api-docs", "/info", "/swagger");
    }

  }
}
