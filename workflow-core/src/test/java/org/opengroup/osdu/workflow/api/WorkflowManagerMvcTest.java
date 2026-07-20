/*
 *  Copyright 2020-2026 Google LLC
 *  Copyright 2020-2026 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.exception.handler.ConflictApiError;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.provider.interfaces.IAdminAuthorizationService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
import org.opengroup.osdu.workflow.security.AuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests for {@link WorkflowManagerApi}
 */
@WebMvcTest(WorkflowManagerApi.class)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Import({AuthorizationFilter.class, DpsHeaders.class})
public class WorkflowManagerMvcTest {
  private static final String TEST_AUTH = "Bearer bla";
  private static final String PARTITION = "partition";
  private static final String CORRELATION_ID = "sample-correlation-id";
  private static final String WORKFLOW_RESPONSE = "{\n" +
      "  \"workflowId\": \"2afccfb8-1351-41c6-9127-61f2d7f22ff8\",\n" +
      "  \"workflowName\": \"HelloWorld\",\n" +
      "  \"description\": \"This is a test workflow\",\n" +
      "  \"creationTimestamp\": 1600144876028,\n" +
      "  \"createdBy\": \"user@email.com\",\n" +
      "  \"version\": 1\n" +
      "}";
  private static final String WORKFLOW_METADATA_LIST_RESPONSE = "[\n" +
      "    {\n" +
      "        \"workflowId\": \"2afccfb8-1351-41c6-9127-61f2d7f22ff8\",\n" +
      "        \"workflowName\": \"HelloWorld\",\n" +
      "        \"description\": \"This is a test workflow\",\n" +
      "        \"creationTimestamp\": 1600144876028,\n" +
      "        \"version\": 1,\n" +
      "        \"registrationInstructions\": {\n" +
      "            \"active\": true,\n" +
      "            \"concurrentWorkflowRun\": 5,\n" +
      "            \"concurrentTaskRun\": 5\n" +
      "        }\n" +
      "    }\n" +
      "]";
  private static final String WORKFLOW_REQUEST = """
      {
        "workflowName": "HelloWorld",
        "description": "This is a test workflow",
        "registrationInstructions": {}
      }""";
  private static final String WORKFLOW_ENDPOINT = "/v1/workflow";
  private static final String EXISTING_WORKFLOW_ID = "existing-id";
  private static final String WORKFLOW_NAME = "test-dag-name";
  private static final String WORKFLOW_ID = "2afccfb8-1351-41c6-9127-61f2d7f22ff8";
  private static final String EMPTY_PREFIX_ERROR =
      "Prefix cannot be Null or Empty. Please provide a value.";

  @Autowired
  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

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

  @MockBean
  private AuthorizationResponse authorizationResponse;

  @InjectMocks
  private WorkflowManagerApi workflowManagerApi;
  @Autowired
  private WebApplicationContext context;
  @Before
  public void setup() {
    //MockitoAnnotations.initMocks(this);
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
    dpsHeaders.put("data-partition-id", "common");
   // mockMvc = MockMvcBuilders.standaloneSetup(workflowManagerApi).build();
  }

  @Test
  public void testCreateApiWithSuccess() throws Exception {
    final CreateWorkflowRequest request = mapper
        .readValue(WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    final WorkflowMetadata metadata = mapper.readValue(WORKFLOW_RESPONSE, WorkflowMetadata.class);
    when(workflowManagerService.createWorkflow(eq(request))).thenReturn(metadata);
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN)))
        .thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        post(WORKFLOW_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .content(WORKFLOW_REQUEST))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowManagerService, times(1)).createWorkflow(eq(request));
    verify(authorizationService, times(1)).authorizeAny(any(), eq(WorkflowRole.ADMIN));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowMetadata responseMetadata =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowMetadata.class);
    assertThat(metadata, equalTo(responseMetadata));
  }

  @Test
  public void testCreateApiWithConflict() throws Exception {
    final CreateWorkflowRequest request = mapper.readValue(WORKFLOW_REQUEST, CreateWorkflowRequest.class);
    when(workflowManagerService.createWorkflow(eq(request)))
        .thenThrow(new ResourceConflictException(EXISTING_WORKFLOW_ID, "conflict"));
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN))).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        post(WORKFLOW_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .content(WORKFLOW_REQUEST))
        .andExpect(status().isConflict())
        .andReturn();
    verify(workflowManagerService, times(1)).createWorkflow(eq(request));
    verify(authorizationService, times(1)).authorizeAny(any(), eq(WorkflowRole.ADMIN));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final ConflictApiError response =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), ConflictApiError.class);
    Assertions.assertEquals(EXISTING_WORKFLOW_ID, response.getConflictId());
  }

  @Test
  public void testGetApiWithSuccess() throws Exception {
    final WorkflowMetadata metadata = mapper.readValue(WORKFLOW_RESPONSE, WorkflowMetadata.class);
    when(workflowManagerService.getWorkflowByName(eq(WORKFLOW_NAME))).thenReturn(metadata);
    when(authorizationService.authorizeAny(any(), any())).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    final MvcResult mvcResult = mockMvc.perform(
        get(WORKFLOW_ENDPOINT + "/{workflow_name}", WORKFLOW_NAME)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andReturn();
    verify(workflowManagerService).getWorkflowByName(eq(WORKFLOW_NAME));
    verify(authorizationService).authorizeAny(any(), any());
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
    final WorkflowMetadata responseMetadata =
        mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), WorkflowMetadata.class);
    assertThat(metadata, equalTo(responseMetadata));
  }

  @Test
  public void testDeleteApiWithSuccess() throws Exception {
    doNothing().when(workflowManagerService).deleteWorkflow(eq(WORKFLOW_NAME));
    when(authorizationService.authorizeAny(any(), eq(WorkflowRole.ADMIN)))
        .thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    mockMvc.perform(
        delete("/v1/workflow/{workflow_name}", WORKFLOW_NAME)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().is(204))
        .andReturn();
    verify(workflowManagerService).deleteWorkflow(eq(WORKFLOW_NAME));
    verify(authorizationService).authorizeAny(any(), eq(WorkflowRole.ADMIN));
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
  }

  @Test
  public void testDeleteApiWithError() throws Exception {
    doThrow(new WorkflowNotFoundException("not found")).when(workflowManagerService)
        .deleteWorkflow(eq(WORKFLOW_NAME));
    when(authorizationService.authorizeAny(any(), any())).thenReturn(authorizationResponse);
    when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
    when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
    when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
    mockMvc.perform(
        delete("/v1/workflow/{workflow_name}", WORKFLOW_NAME)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getHttpHeaders())
            .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isNotFound())
        .andReturn();
    verify(workflowManagerService).deleteWorkflow(eq(WORKFLOW_NAME));
    verify(authorizationService).authorizeAny(any(), any());
    verify(dpsHeaders).getAuthorization();
    verify(dpsHeaders).getPartitionId();
  }

   @Test
   public void testGetAllWorkflowsWithSuccess() throws Exception {
     final List<WorkflowMetadata> workflowMetadataList =
         mapper.readValue(WORKFLOW_METADATA_LIST_RESPONSE, List.class);
     when(workflowManagerService.getAllWorkflowForTenant(eq("Hello"))).
         thenReturn(workflowMetadataList);
     when(authorizationService.authorizeAny(any(), eq(WorkflowRole.VIEWER), eq(WorkflowRole.CREATOR),
         eq(WorkflowRole.ADMIN))).thenReturn(authorizationResponse);
     when(dpsHeaders.getAuthorization()).thenReturn(TEST_AUTH);
     when(dpsHeaders.getPartitionId()).thenReturn(PARTITION);
     when(dpsHeaders.getCorrelationId()).thenReturn(CORRELATION_ID);
     final MvcResult mvcResult = mockMvc.perform(
         get( String.format("/v1/workflow/?prefix=%s","Hello"))
             .contentType(MediaType.APPLICATION_JSON)
             .headers(getHttpHeaders())
             .with(SecurityMockMvcRequestPostProcessors.csrf()))
         .andExpect(status().isOk())
         .andReturn();
     verify(workflowManagerService).getAllWorkflowForTenant(eq("Hello"));
     verify(authorizationService).authorizeAny(any(), eq(WorkflowRole.VIEWER), eq(WorkflowRole.CREATOR),
         eq(WorkflowRole.ADMIN));
     verify(dpsHeaders).getAuthorization();
     verify(dpsHeaders).getPartitionId();
     final List<WorkflowMetadata> responseWorkflowMetadataList =
         mapper.readValue(mvcResult.getResponse().getContentAsByteArray(), List.class);
     assertThat(workflowMetadataList,equalTo(responseWorkflowMetadataList));
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
          .httpBasic(AbstractHttpConfigurer::disable);
      return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
      return (web) -> web.ignoring().requestMatchers("/api-docs", "/info", "/swagger");
    }

  }


}
