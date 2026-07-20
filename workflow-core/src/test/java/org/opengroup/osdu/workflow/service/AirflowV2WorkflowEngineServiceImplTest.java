/*
 *  Copyright 2020-2025 Google LLC
 *  Copyright 2020-2025 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AirflowV2WorkflowEngineServiceImplTest {

  private static final String TEST_RUN_ID = "1efe8c03-c087-4ae8-a3f3-a31975184165";
  private static final String TEST_DAG_NAME = "test_dag";
  private static final String TEST_WORKFLOW_NAME = "test_workflow";
  private static final String TEST_USER_EMAIL = "user@osdu.group";
  private static final String TEST_VERSION = "2.10.2+composer";
  private static final String RESPONSE_EXECUTION_DATE = "2025-05-29T12:34:56.000Z";
  private static final String RESPONSE_DAG_RUN_ID = "dagRunId-123";
  private static final String VERSION_RESPONSE_JSON = "{\"version\":\"2.10.2+composer\"}";
  private static final String VERSION_RESPONSE_INVALID = "{}";
  private static final String INVALID_JSON = "{not_a_valid_json---";
  private static final String RESPONSE_JSON =
      String.format(
          "{\"%s\":\"%s\",\"%s\":\"%s\"}",
          AirflowV2WorkflowEngineServiceImpl.EXECUTION_DATE_PARAMETER_NAME,
          RESPONSE_EXECUTION_DATE,
          AirflowV2WorkflowEngineServiceImpl.RUN_ID_PARAMETER_NAME_STABLE,
          RESPONSE_DAG_RUN_ID);
  private static final String EXECUTION_CONTEXT = "execution_context";
  private static final String KEY_USER_ID = "userId";

  @Mock private IAirflowApiClient airflowApiClient;
  @Mock private DpsHeaders dpsHeaders;
  @Mock private ClientResponse clientResponse;

  @InjectMocks private AirflowV2WorkflowEngineServiceImpl service;

  @Test
  void should_ReturnTriggerWorkflowUrl_when_GetTriggerWorkflowUrlCalled() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getDagName()).thenReturn(TEST_DAG_NAME);

    String url = service.getTriggerWorkflowUrl(request);

    String expected =
        String.format(
            AirflowV2WorkflowEngineServiceImpl.TRIGGER_AIRFLOW_ENDPOINT_STABLE, TEST_DAG_NAME);
    assertEquals(expected, url);
  }

  @Test
  void should_ReturnWorkflowRunStatusUrl_when_GetWorkflowRunStatusUrlCalled() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getDagName()).thenReturn(TEST_DAG_NAME);
    when(request.getRunId()).thenReturn(TEST_RUN_ID);

    String url = service.getWorkflowRunStatusUrl(request);

    String expected =
        String.format(
            AirflowV2WorkflowEngineServiceImpl.AIRFLOW_RUN_ENDPOINT_STABLE,
            TEST_DAG_NAME,
            TEST_RUN_ID);
    assertEquals(expected, url);
  }

  @Test
  void should_ReturnTriggerWorkflowResponse_when_GetTriggerWorkflowResponseWithValidJson() {
    when(clientResponse.getResponseBody()).thenReturn(RESPONSE_JSON);

    TriggerWorkflowResponse resp = service.getTriggerWorkflowResponse(clientResponse);

    assertEquals(RESPONSE_EXECUTION_DATE, resp.getExecutionDate());
    assertEquals(RESPONSE_DAG_RUN_ID, resp.getRunId());
  }

  @Test
  void should_ThrowAppException_when_GetTriggerWorkflowResponseWithInvalidJson() {
    when(clientResponse.getResponseBody()).thenReturn(INVALID_JSON);

    AppException exception =
        assertThrows(AppException.class, () -> service.getTriggerWorkflowResponse(clientResponse));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getError().getCode());
    assertEquals("Unable to Process(Parse, Generate) JSON value", exception.getError().getReason());
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_AddUserIdToExecutionContext_when_TriggerWorkflowCalledAndInputIsValid() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getDagName()).thenReturn(TEST_DAG_NAME);
    when(request.getRunId()).thenReturn(TEST_RUN_ID);
    when(request.getWorkflowName()).thenReturn(TEST_WORKFLOW_NAME);

    when(dpsHeaders.getUserEmail()).thenReturn(TEST_USER_EMAIL);

    Map<String, Object> executionContext = new HashMap<>();
    Map<String, Object> context = new HashMap<>();
    context.put(EXECUTION_CONTEXT, executionContext);

    when(clientResponse.getResponseBody()).thenReturn(VERSION_RESPONSE_JSON);
    when(airflowApiClient.callAirflow(any(), any(), any(), any(), any()))
        .thenReturn(clientResponse);

    service.triggerWorkflow(request, context);

    assertEquals(
        TEST_USER_EMAIL, ((Map<String, Object>) context.get(EXECUTION_CONTEXT)).get(KEY_USER_ID));
  }

  @Test
  void should_ThrowAppException_when_AddUserIdToExecutionContextWithNullContext() {
    Map<String, Object> nullMap = null;
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);

    AppException ex =
        assertThrows(
            AppException.class, () -> service.addUserIdToExecutionContext(nullMap, request));
    assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getError().getCode());
    assertEquals("Failed to trigger workflow run", ex.getError().getReason());
  }

  @Test
  void should_ThrowAppException_when_AddUserIdToExecutionContextWithNullExecContext() {
    Map<String, Object> map = new HashMap<>();
    map.put(EXECUTION_CONTEXT, null);
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);

    AppException ex =
        assertThrows(AppException.class, () -> service.addUserIdToExecutionContext(map, request));
    assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getError().getCode());
    assertEquals("execution_context is null or empty", ex.getMessage());
  }

  @Test
  void should_ThrowAppException_when_AddUserIdToExecutionContextWhenUserIdAlreadyPresent() {
    Map<String, Object> executionContext = new HashMap<>();
    executionContext.put(KEY_USER_ID, TEST_USER_EMAIL);
    Map<String, Object> map = new HashMap<>();
    map.put(EXECUTION_CONTEXT, executionContext);

    WorkflowEngineRequest req = mock(WorkflowEngineRequest.class);
    when(req.getWorkflowName()).thenReturn(TEST_WORKFLOW_NAME);

    AppException ex =
        assertThrows(AppException.class, () -> service.addUserIdToExecutionContext(map, req));
    assertEquals(400, ex.getError().getCode());
  }

  @Test
  void should_ReturnVersion_when_GetVersionWithVersionPresent() {
    when(clientResponse.getResponseBody()).thenReturn(VERSION_RESPONSE_JSON);
    when(airflowApiClient.callAirflow(any(), any(), any(), any(), any()))
        .thenReturn(clientResponse);

    Optional<String> version = service.getVersion();

    assertTrue(version.isPresent());
    assertEquals(TEST_VERSION, version.get());
  }

  @Test
  void should_ReturnNotAvailable_when_GetVersionWithNoVersionInResponse() {
    when(clientResponse.getResponseBody()).thenReturn(VERSION_RESPONSE_INVALID);
    when(airflowApiClient.callAirflow(any(), any(), any(), any(), any()))
        .thenReturn(clientResponse);

    Optional<String> version = service.getVersion();

    assertTrue(version.isPresent());
    assertEquals(AirflowV2WorkflowEngineServiceImpl.NOT_AVAILABLE, version.get());
  }

  @Test
  void should_ReturnNotAvailable_when_GetVersionWithJsonParseError() {
    when(clientResponse.getResponseBody()).thenReturn(INVALID_JSON);
    when(airflowApiClient.callAirflow(any(), any(), any(), any(), any()))
        .thenReturn(clientResponse);

    Optional<String> version = service.getVersion();

    assertTrue(version.isPresent());
    assertEquals(AirflowV2WorkflowEngineServiceImpl.NOT_AVAILABLE, version.get());
  }
}
