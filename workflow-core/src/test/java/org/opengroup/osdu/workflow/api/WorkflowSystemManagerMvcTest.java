package org.opengroup.osdu.workflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.exception.handler.ConflictApiError;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAdminAuthorizationService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WorkflowSystemManagerApi}
 */
@WebMvcTest(WorkflowSystemManagerApi.class)
@AutoConfigureMockMvc
@Import({AuthorizationFilter.class, DpsHeaders.class})
class WorkflowSystemManagerMvcTest {
  private static final String TEST_AUTH = "Bearer bla";
  private static final String CORRELATION_ID = "sample-correlation-id";
  private static final String WORKFLOW_RESPONSE = "{\n" +
      "  \"workflowId\": \"2afccfb8-1351-41c6-9127-61f2d7f22ff8\",\n" +
      "  \"workflowName\": \"HelloWorld\",\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"creationTimestamp\": 1600144876028,\n" +
      "  \"createdBy\": \"user@email.com\",\n" +
      "  \"version\": 1\n" +
      "}";
  private static final String WORKFLOW_REQUEST = "{\n" +
      "  \"workflowName\": \"HelloWorld\",\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"registrationInstructions\": {\n" +
      "          \"active\": true,\n" +
      "          \"concurrentWorkflowRun\": 5,\n" +
      "          \"concurrentTaskRun\": 5\n" +
      "      }\n" +
      "}";
  private static final String SYSTEM_WORKFLOW_ENDPOINT = "/v1/workflow/system";
  private static final String EXISTING_WORKFLOW_ID = "existing-id";
  private static final String WORKFLOW_NAME = "test-dag-name";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private IWorkflowManagerService workflowManagerService;

  @MockBean
  private IAuthorizationService authorizationService;

  @MockBean
  private IAdminAuthorizationService adminAuthorizationService;

  @MockBean
  private JaxRsDpsLog log;

  @MockBean
  private DpsHeaders dpsHeaders;

  @Mock
  private AuthorizationResponse authorizationResponse;

  @Test
  void testCreateSystemApiWithSuccess() throws Exception {
    final CreateWorkflowRequest request = mapper
        .readValue(WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    final WorkflowMetadata metadata = mapper.readValue(WORKFLOW_RESPONSE, WorkflowMetadata.class);
    when(workflowManagerService.createSystemWorkflow(eq(request))).thenReturn(metadata);
    when(adminAuthorizationService.isDomainAdminServiceAccount())
        .thenReturn(true);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn("");
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
            post(SYSTEM_WORKFLOW_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(getHttpHeadersWithoutDataPartitionId())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(WORKFLOW_REQUEST))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowManagerService).createSystemWorkflow(eq(request));
    verify(adminAuthorizationService).isDomainAdminServiceAccount();
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowMetadata responseMetadata =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowMetadata.class);
    assertThat(metadata, equalTo(responseMetadata));
  }

  @Test
  public void testCreateSystemApiWithConflict() throws Exception {
    final CreateWorkflowRequest request = mapper.readValue(WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    when(workflowManagerService.createSystemWorkflow(eq(request)))
        .thenThrow(new ResourceConflictException(EXISTING_WORKFLOW_ID, "conflict"));
    when(adminAuthorizationService.isDomainAdminServiceAccount())
        .thenReturn(true);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn("");
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
            post(SYSTEM_WORKFLOW_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(getHttpHeadersWithoutDataPartitionId())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(WORKFLOW_REQUEST))
        .andExpect(status().isConflict())
        .andReturn();
    verify(workflowManagerService).createSystemWorkflow(eq(request));
    when(adminAuthorizationService.isDomainAdminServiceAccount())
        .thenReturn(true);
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final ConflictApiError response =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), ConflictApiError.class);
    Assertions.assertEquals(EXISTING_WORKFLOW_ID, response.getConflictId());
  }

  @Test
  void testDeleteSystemApiWithSuccess() throws Exception {
    doNothing().when(workflowManagerService).deleteSystemWorkflow(eq(WORKFLOW_NAME));
    when(adminAuthorizationService.isDomainAdminServiceAccount())
        .thenReturn(true);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    mockMvc.perform(
            delete("/v1/workflow/system/{workflow_name}", WORKFLOW_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(getHttpHeadersWithoutDataPartitionId())
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().is(204))
        .andReturn();
    verify(workflowManagerService).deleteSystemWorkflow(eq(WORKFLOW_NAME));
    verify(adminAuthorizationService).isDomainAdminServiceAccount();
    verify(dpsHeaders).getAuthorization();
  }

  @Test
  void testDeleteSystemApiWithError() throws Exception {
    doThrow(new WorkflowNotFoundException("not found")).when(workflowManagerService)
        .deleteSystemWorkflow(eq(WORKFLOW_NAME));
    when(adminAuthorizationService.isDomainAdminServiceAccount())
        .thenReturn(true);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn("");
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    mockMvc.perform(
            delete("/v1/workflow/system/{workflow_name}", WORKFLOW_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(getHttpHeadersWithoutDataPartitionId())
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isNotFound())
        .andReturn();
    verify(workflowManagerService).deleteSystemWorkflow(eq(WORKFLOW_NAME));
    verify(adminAuthorizationService).isDomainAdminServiceAccount();
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
  }

  private HttpHeaders getHttpHeadersWithoutDataPartitionId() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(DpsHeaders.AUTHORIZATION, TEST_AUTH);
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
