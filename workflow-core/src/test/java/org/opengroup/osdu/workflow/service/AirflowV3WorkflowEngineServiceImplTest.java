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
import org.json.JSONObject;
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
class AirflowV3WorkflowEngineServiceImplTest {

  private static final String TEST_RUN_ID = "1efe8c03-c087-4ae8-a3f3-a31975184165";
  private static final String TEST_DAG_NAME = "test_dag";
  private static final String TEST_VERSION = "3.0.1";
  private static final String RESPONSE_LOGICAL_DATE = "2025-05-29T12:34:56.000Z";
  private static final String RESPONSE_DAG_RUN_ID = "dagRunId-123";
  private static final String VERSION_RESPONSE_JSON = "{\"version\":\"3.0.1\"}";
  private static final String INVALID_JSON = "{not_a_valid_json---";
  private static final String RESPONSE_JSON =
      String.format(
          "{\"%s\":\"%s\",\"%s\":\"%s\"}",
          AirflowV3WorkflowEngineServiceImpl.LOGICAL_DATE_PARAMETER_NAME,
          RESPONSE_LOGICAL_DATE,
          AirflowV3WorkflowEngineServiceImpl.RUN_ID_PARAMETER_NAME,
          RESPONSE_DAG_RUN_ID);

  @Mock private IAirflowApiClient airflowApiClient;
  @Mock private DpsHeaders dpsHeaders;
  @Mock private ClientResponse clientResponse;

  @InjectMocks private AirflowV3WorkflowEngineServiceImpl service;

  @Test
  void should_UseApiV2TriggerUrl() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getDagName()).thenReturn(TEST_DAG_NAME);

    String url = service.getTriggerWorkflowUrl(request);

    assertEquals("api/v2/dags/" + TEST_DAG_NAME + "/dagRuns", url);
  }

  @Test
  void should_UseApiV2StatusUrl() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getDagName()).thenReturn(TEST_DAG_NAME);
    when(request.getRunId()).thenReturn(TEST_RUN_ID);

    String url = service.getWorkflowRunStatusUrl(request);

    assertEquals("api/v2/dags/" + TEST_DAG_NAME + "/dagRuns/" + TEST_RUN_ID, url);
  }

  @Test
  void should_IncludeLogicalDateAndRunId_inRequestBody() {
    WorkflowEngineRequest request = mock(WorkflowEngineRequest.class);
    when(request.getRunId()).thenReturn(TEST_RUN_ID);

    JSONObject body = service.getTriggerWorkflowRequestBody(request, new HashMap<>());

    assertEquals(TEST_RUN_ID, body.getString(AirflowV3WorkflowEngineServiceImpl.RUN_ID_PARAMETER_NAME));
    assertTrue(body.has(AirflowV3WorkflowEngineServiceImpl.LOGICAL_DATE_PARAMETER_NAME));
    assertTrue(body.has(AirflowV3WorkflowEngineServiceImpl.AIRFLOW_PAYLOAD_PARAMETER_NAME));
  }

  @Test
  void should_ParseLogicalDateAndRunId_fromTriggerResponse() {
    when(clientResponse.getResponseBody()).thenReturn(RESPONSE_JSON);

    TriggerWorkflowResponse resp = service.getTriggerWorkflowResponse(clientResponse);

    assertEquals(RESPONSE_LOGICAL_DATE, resp.getExecutionDate());
    assertEquals(RESPONSE_DAG_RUN_ID, resp.getRunId());
  }

  @Test
  void should_ThrowAppException_when_TriggerResponseInvalidJson() {
    when(clientResponse.getResponseBody()).thenReturn(INVALID_JSON);

    AppException exception =
        assertThrows(AppException.class, () -> service.getTriggerWorkflowResponse(clientResponse));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getError().getCode());
  }

  @Test
  void should_ReturnVersion_from_ApiV2VersionEndpoint() {
    when(clientResponse.getResponseBody()).thenReturn(VERSION_RESPONSE_JSON);
    when(airflowApiClient.callAirflow(any(), any(), any(), any(), any()))
        .thenReturn(clientResponse);

    Optional<String> version = service.getVersion();

    assertTrue(version.isPresent());
    assertEquals(TEST_VERSION, version.get());
  }
}
