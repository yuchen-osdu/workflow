// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.workflow.provider.azure.utils.airflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareConfig;

@ExtendWith(MockitoExtension.class)
public class AirflowV3WorkflowEngineUtilTest {

  private static final ObjectMapper REAL_MAPPER = new ObjectMapper();

  @Mock private ObjectMapper objectMapper;
  @Mock private FileShareConfig fileShareConfig;
  @Mock private WorkflowEngineRequest workflowEngineRequest;

  @InjectMocks private AirflowV3WorkflowEngineUtil util;

  @Test
  public void testApiV2Urls() {
    assertEquals("api/v2/dags/%s", util.getAirflowDagsUrl());
    assertEquals("api/v2/dags/%s/dagRuns", util.getAirflowDagRunsUrl());
    assertEquals("api/v2/dags/%s/dagRuns/%s", util.getAirflowDagRunsStatusUrl());
  }

  @Test
  public void testDagRunIdParameterNameIsDagRunId() {
    assertEquals("dag_run_id", util.getDagRunIdParameterName());
  }

  @Test
  public void testGetFileShareNameUsesAirflow3ShareName() {
    when(fileShareConfig.getAirflow3ShareName()).thenReturn("airflow3dags");
    assertEquals("airflow3dags", util.getFileShareName(fileShareConfig));
  }

  @Test
  public void testGetFileShareNameFallsBackToAirflow2() {
    when(fileShareConfig.getAirflow3ShareName()).thenReturn("");
    when(fileShareConfig.getAirflow2ShareName()).thenReturn("airflow2dags");
    assertEquals("airflow2dags", util.getFileShareName(fileShareConfig));
  }

  @Test
  public void testExtractTriggerWorkflowResponseParsesLogicalDateAndRunId()
      throws JsonProcessingException {
    String response = "{\"logical_date\":\"2025-05-29T12:00:00Z\",\"dag_run_id\":\"run-1\"}";
    JsonNode node = REAL_MAPPER.readTree(response);
    when(objectMapper.readValue(eq(response), eq(JsonNode.class))).thenReturn(node);

    TriggerWorkflowResponse resp = util.extractTriggerWorkflowResponse(response);

    assertEquals("2025-05-29T12:00:00Z", resp.getExecutionDate());
    assertEquals("run-1", resp.getRunId());
  }

  @Test
  public void testExtractActiveDagRunsResponse() throws JsonProcessingException {
    String response = "{\"active_dag_runs\":7}";
    when(objectMapper.readValue(eq(response), eq(JsonNode.class)))
        .thenReturn(REAL_MAPPER.readTree(response));
    assertEquals(7, util.extractActiveDagRunsResponse(response));
  }

  @Test
  public void testGetDagRunIdentificationParamUsesRunId() {
    when(workflowEngineRequest.getRunId()).thenReturn("run-9");
    assertEquals("run-9", util.getDagRunIdentificationParam(workflowEngineRequest));
  }

  @Test
  public void testAddMicroSecParamAddsLogicalDateWhenAbsent() {
    JSONObject body = new JSONObject();
    JSONObject result = util.addMicroSecParam(body);
    assertTrue(result.has("logical_date"));
  }

  @Test
  public void testAddMicroSecParamKeepsExistingLogicalDate() {
    JSONObject body = new JSONObject();
    body.put("logical_date", "fixed-date");
    JSONObject result = util.addMicroSecParam(body);
    assertEquals("fixed-date", result.getString("logical_date"));
    assertFalse("fixed-date".isEmpty());
  }
}
