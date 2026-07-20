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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.*;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AbstractAirflowWorkflowEngineServiceTest {
  private static final String TEST_WORKFLOW_ID = "test_workflow_id";
  private static final String TEST_WORKFLOW_NAME = "test_workflow";
  private static final Map<String, Object> TEST_CONTEXT = Map.of("test_key", "test_value");
  private static final String EXPECTED_FAILED_TO_TRIGGER_ERROR_MESSAGE =
      "Failed to trigger workflow with id %s and name %s"
          .formatted(TEST_WORKFLOW_ID, TEST_WORKFLOW_NAME);
  private static final String TEST_EXECUTION_DATE = "2025-05-29T12:34:56";
  private static final long TEST_EXECUTION_TIMESTAMP = 1748522096000L;
  private static final String EXPECTED_NO_WORKFLOW_EXECUTED_ERROR_MESSAGE =
      "No WorkflowRun executed for Workflow: %s on %s "
          .formatted(TEST_WORKFLOW_NAME, TEST_EXECUTION_DATE);
  private static final String URL = "url";
  private static final String STATUS_URL = "status-url";

  @Mock private IAirflowApiClient airflowApiClient;

  private TestWorkflowEngineService service;

  @BeforeEach
  void setup() {
    service = new TestWorkflowEngineService(airflowApiClient);
  }

  @Test
  void should_TriggerWorkflow_and_ReturnResponse_when_AirflowApiSucceeds() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getWorkflowId()).thenReturn(TEST_WORKFLOW_ID);
    when(request.getWorkflowName()).thenReturn(TEST_WORKFLOW_NAME);

    ClientResponse mockResponse = mock(ClientResponse.class);

    TriggerWorkflowResponse expectedResponse = mock(TriggerWorkflowResponse.class);
    service.triggerWorkflowResponse = expectedResponse;

    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);

    when(airflowApiClient.callAirflow(
            eq(HttpMethod.POST), any(), any(), eq(request), errorMessageCaptor.capture()))
        .thenReturn(mockResponse);

    TriggerWorkflowResponse result = service.triggerWorkflow(request, TEST_CONTEXT);

    assertEquals(expectedResponse, result);
    assertEquals(EXPECTED_FAILED_TO_TRIGGER_ERROR_MESSAGE, errorMessageCaptor.getValue());
  }

  @Test
  void should_Return_WorkflowStatusType_when_AirflowApiReturnsValidStatus() {
    WorkflowStatusType workflowStatusType = WorkflowStatusType.SUCCESS;

    AirflowGetDAGRunStatus dagRunStatus = new AirflowGetDAGRunStatus();
    dagRunStatus.setStatusType(workflowStatusType);
    String jsonDagRunStatus;
    try {
      jsonDagRunStatus = new ObjectMapper().writeValueAsString(dagRunStatus);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    ClientResponse response = mock(ClientResponse.class);
    when(response.getResponseBody()).thenReturn(jsonDagRunStatus);
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getWorkflowName()).thenReturn(TEST_WORKFLOW_NAME);
    when(request.getExecutionTimeStamp()).thenReturn(TEST_EXECUTION_TIMESTAMP);

    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);

    when(airflowApiClient.callAirflow(
            eq(HttpMethod.GET), any(), any(), eq(request), errorMessageCaptor.capture()))
        .thenReturn(response);

    WorkflowStatusType result = service.getWorkflowRunStatus(request);

    assertEquals(workflowStatusType, result);
    assertEquals(EXPECTED_NO_WORKFLOW_EXECUTED_ERROR_MESSAGE, errorMessageCaptor.getValue());
  }

  @Test
  void should_ThrowAppException_when_getAirflowGetDAGRunStatus_invalidJson() {
    ClientResponse response = mock(ClientResponse.class);
    when(response.getResponseBody()).thenReturn("invalid-json");

    AppException exception =
        assertThrows(AppException.class, () -> service.getAirflowGetDAGRunStatus(response));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getError().getCode());
  }

  static class TestWorkflowEngineService extends AbstractAirflowWorkflowEngineService {
    TriggerWorkflowResponse triggerWorkflowResponse;

    TestWorkflowEngineService(IAirflowApiClient airflowApiClient) {
      super(airflowApiClient);
    }

    @Override
    protected JSONObject getTriggerWorkflowRequestBody(
        WorkflowEngineRequest rq, Map<String, Object> context) {
      return new JSONObject(TEST_CONTEXT);
    }

    @Override
    protected String getTriggerWorkflowUrl(WorkflowEngineRequest rq) {
      return URL;
    }

    @Override
    protected TriggerWorkflowResponse getTriggerWorkflowResponse(ClientResponse airflowResponse) {
      return triggerWorkflowResponse;
    }

    @Override
    protected String getWorkflowRunStatusUrl(WorkflowEngineRequest rq) {
      return STATUS_URL;
    }
  }
}
