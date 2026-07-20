package org.opengroup.osdu.workflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.workflow.exception.WorkflowRunCompletedException;
import org.opengroup.osdu.workflow.exception.handler.RestExceptionHandler;
import org.opengroup.osdu.workflow.model.UpdateWorkflowRunRequest;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.model.WorkflowRunResponse;
import org.opengroup.osdu.workflow.provider.interfaces.IAdminAuthorizationService;
import org.opengroup.osdu.workflow.security.AuthorizationFilter;
import org.opengroup.osdu.workflow.model.TriggerWorkflowRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunService;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WorkflowRunApi}
 */
@WebMvcTest(WorkflowRunApi.class)
@AutoConfigureMockMvc
@Import({AuthorizationFilter.class, DpsHeaders.class})
class WorkflowRunMvcTest {
  private static final String TEST_AUTH = "Bearer bla";
  private static final String PARTITION = "partition";
  private static final String WORKFLOW_NAME = "test-dag-name";
  private static final String RUN_ID = "d13f7fd0-d27e-4176-8d60-6e9aad86e347";
  private static final String CORRELATION_ID = "sample-correlation-id";
  private static final String TRIGGER_WORKFLOW_ENDPOINT = String
      .format("/v1/workflow/%s/workflowRun", WORKFLOW_NAME);
  private static final String TRIGGER_WORKFLOW_REQUEST = "{\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"executionContext\": {\n" +
      "    \"id\": \"someid\",\n" +
      "    \"kind\": \"somekind\",\n" +
      "    \"dataPartitionId\": \"someId\"\n" +
      "  }\n" +
      "}";
  private static final String WORKFLOW_RUN_RESPONSE = "{\n" +
      "  \"workflowId\": \"2afccfb8-1351-41c6-9127-61f2d7f22ff8\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"startTimeStamp\": 1600145420675,\n" +
      "  \"endTimeStamp\": 1600145420675,\n" +
      "  \"status\": \"submitted\",\n" +
      "  \"submittedBy\": \"user@mail.com\"\n" +
      "}";
  private static final String WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA = "{\n" +
      "  \"status\": \"finished\"\n" +
      "}";
  private static final String WORKFLOW_RUN_UPDATE_RUNNING_STATUS_REQUEST_DATA = "{\n" +
      "  \"status\": \"running\"\n" +
      "}";
  private static final String STATUS_FINISHED_WORKFLOW_RUN_RESPONSE = "{\n" +
      "  \"workflowId\" : \"SGVsbG9Xb3JsZA==\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"startTimeStamp\": 1607430997362,\n" +
      "  \"endTimeStamp\": 1607936756808,\n" +
      "  \"status\": \"finished\",\n" +
      "  \"submittedBy\": \"user@email.com\"\n" +
      "}";
  private static final String STATUS_RUNNING_WORKFLOW_RUN_RESPONSE = "{\n" +
      "  \"workflowId\" : \"SGVsbG9Xb3JsZA==\",\n" +
      "  \"runId\": \"d13f7fd0-d27e-4176-8d60-6e9aad86e347\",\n" +
      "  \"startTimeStamp\": 1607430997362,\n" +
      "  \"status\": \"running\",\n" +
      "  \"submittedBy\": \"user@email.com\"\n" +
      "}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private IWorkflowRunService workflowRunService;
  @MockBean
  private IAuthorizationService authorizationService;
  @MockBean
  private IAdminAuthorizationService adminAuthorizationService;
  @MockBean
  private RestExceptionHandler restExceptionHandler;
  @MockBean
  private DpsHeaders dpsHeaders;
  @MockBean
  private JaxRsDpsLog logger;
  @MockBean
  private AuthorizationResponse authorizationResponse;

  @Test
  void testTriggerWorkflowApiWithSuccess() throws Exception {
    final TriggerWorkflowRequest request = mapper
        .readValue(TRIGGER_WORKFLOW_REQUEST, TriggerWorkflowRequest.class);
    final WorkflowRunResponse workflowRunResponse = mapper
        .readValue(WORKFLOW_RUN_RESPONSE, WorkflowRunResponse.class);
    when(workflowRunService.triggerWorkflow(eq(WORKFLOW_NAME), eq(request)))
        .thenReturn(workflowRunResponse);
    when(authorizationService.authorizeAny(any(), any())).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        post(TRIGGER_WORKFLOW_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .content(TRIGGER_WORKFLOW_REQUEST))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowRunService, times(1)).triggerWorkflow(eq(WORKFLOW_NAME), eq(request));
    verify(authorizationService, times(1)).authorizeAny(any(), any());
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowRunResponse response = mapper
        .readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowRunResponse.class);
    assertThat(workflowRunResponse, equalTo(response));
  }

  @Test
  void testGetWorkflowRunApiWithSuccess() throws Exception {
    final WorkflowRunResponse workflowRunResponse = mapper
        .readValue(WORKFLOW_RUN_RESPONSE, WorkflowRunResponse.class);
    when(workflowRunService.getWorkflowRunByName(eq(WORKFLOW_NAME), eq(RUN_ID)))
        .thenReturn(workflowRunResponse);
    when(authorizationService.authorizeAny(any(), any())).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        get("/v1/workflow/{workflow_name}/workflowRun/{runId}", WORKFLOW_NAME, RUN_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowRunService).getWorkflowRunByName(eq(WORKFLOW_NAME), eq(RUN_ID));
    verify(authorizationService).authorizeAny(any(), any());
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowRunResponse responseWorkflowRun =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowRunResponse.class);
    assertThat(workflowRunResponse, equalTo(responseWorkflowRun));

  }

  @Test
  public void testUpdateWorkflowRunStatusApiWithSuccessTypeRunning() throws Exception {
    final WorkflowRunResponse workflowRunResponse = mapper
        .readValue(STATUS_RUNNING_WORKFLOW_RUN_RESPONSE, WorkflowRunResponse.class);
    final UpdateWorkflowRunRequest request = mapper
        .readValue(WORKFLOW_RUN_UPDATE_RUNNING_STATUS_REQUEST_DATA, UpdateWorkflowRunRequest.class);
    when(workflowRunService.updateWorkflowRunStatus(eq(WORKFLOW_NAME), eq(RUN_ID),
        eq(request.getStatus()))).thenReturn(workflowRunResponse);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.VIEWER),
        eq(WorkflowRole.CREATOR))).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        put("/v1/workflow/{workflow_name}/workflowRun/{runId}", WORKFLOW_NAME, RUN_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .content(WORKFLOW_RUN_UPDATE_RUNNING_STATUS_REQUEST_DATA))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowRunService).updateWorkflowRunStatus(eq(WORKFLOW_NAME), eq(RUN_ID),
        eq(request.getStatus()));
    verify(authorizationService).authorizeAny(any(), eq(WorkflowRole.VIEWER),
        eq(WorkflowRole.CREATOR));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowRunResponse responseWorkflowRun =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowRunResponse.class);
    assertThat(workflowRunResponse, equalTo(responseWorkflowRun));
  }

  @Test
  public void testUpdateWorkflowRunStatusApiWithSuccessTypeFinished() throws Exception {
    final WorkflowRunResponse workflowRunResponse = mapper
        .readValue(STATUS_FINISHED_WORKFLOW_RUN_RESPONSE, WorkflowRunResponse.class);
    final UpdateWorkflowRunRequest request = mapper
        .readValue(WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA, UpdateWorkflowRunRequest.class);
    when(workflowRunService.updateWorkflowRunStatus(eq(WORKFLOW_NAME), eq(RUN_ID),
        eq(request.getStatus()))).thenReturn(workflowRunResponse);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.VIEWER),
        eq(WorkflowRole.CREATOR))).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        put("/v1/workflow/{workflow_name}/workflowRun/{runId}", WORKFLOW_NAME, RUN_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .content(WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowRunService).updateWorkflowRunStatus(eq(WORKFLOW_NAME), eq(RUN_ID),
        eq(request.getStatus()));
    verify(authorizationService).authorizeAny(any(), eq(WorkflowRole.VIEWER),
        eq(WorkflowRole.CREATOR));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowRunResponse responseWorkflowRun =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowRunResponse.class);
    assertThat(workflowRunResponse, equalTo(responseWorkflowRun));
  }

  @Test
  public void testUpdateWorkflowRunStatusApiWithFailure() throws Exception {
    final UpdateWorkflowRunRequest request = mapper
        .readValue(WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA, UpdateWorkflowRunRequest.class);
    when(workflowRunService.updateWorkflowRunStatus(eq(WORKFLOW_NAME), eq(RUN_ID),
        eq(request.getStatus()))).thenThrow(new WorkflowRunCompletedException(WORKFLOW_NAME,RUN_ID));
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.VIEWER),
        eq(WorkflowRole.CREATOR))).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        put("/v1/workflow/{workflow_name}/workflowRun/{runId}", WORKFLOW_NAME, RUN_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .content(WORKFLOW_RUN_UPDATE_FINISHED_STATUS_REQUEST_DATA))
        .andExpect(status().isBadRequest())
        .andReturn();
    verify(workflowRunService).updateWorkflowRunStatus(eq(WORKFLOW_NAME), eq(RUN_ID),
        eq(request.getStatus()));
    verify(authorizationService).authorizeAny(any(), eq(WorkflowRole.VIEWER),
        eq(WorkflowRole.CREATOR));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    assertTrue(mvcResult.getResolvedException() instanceof WorkflowRunCompletedException);
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
