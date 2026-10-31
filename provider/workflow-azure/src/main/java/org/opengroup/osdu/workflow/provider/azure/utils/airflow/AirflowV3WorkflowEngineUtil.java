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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.json.JSONObject;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Airflow 3 engine util targeting the public {@code api/v2/...} REST endpoints.
 *
 * <p>Registered only when Airflow 3 is enabled ({@code osdu.airflow.version=airflow3}), so that it
 * can coexist with the Airflow 2 util for deterministic per-run routing during migration.
 */
@Component("AirflowV3WorkflowEngineUtil")
@ConditionalOnExpression(
    "T(org.opengroup.osdu.workflow.model.AirflowEngineVersions).isAirflow3('${osdu.airflow.version:airflow2}')")
public class AirflowV3WorkflowEngineUtil implements IAirflowWorkflowEngineUtil {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AirflowV3WorkflowEngineUtil.class);

  private static final String FILE_NAME_SUFFIX = ".py";

  private static final String AIRFLOW_DAGS_URL = "api/v2/dags/%s";
  private static final String AIRFLOW_DAG_RUNS_URL = "api/v2/dags/%s/dagRuns";
  private static final String AIRFLOW_ACTIVE_DAG_RUNS_COUNT_URL = "activeDagRuns/";
  private static final String AIRFLOW_DAG_RUNS_STATUS_URL = "api/v2/dags/%s/dagRuns/%s";

  private static final String RUN_ID_PARAMETER_NAME = "dag_run_id";
  private static final String LOGICAL_DATE_PARAMETER_NAME = "logical_date";
  private static final String ACTIVE_DAG_RUNS = "active_dag_runs";

  @Autowired
  @Qualifier("WorkflowObjectMapper")
  private ObjectMapper objectMapper;

  @Override
  public String getDagRunIdParameterName() {
    return RUN_ID_PARAMETER_NAME;
  }

  @Override
  public String getFileNameFromWorkflow(String workflowName) {
    return workflowName + FILE_NAME_SUFFIX;
  }

  @Override
  public String getAirflowDagsUrl() {
    return AIRFLOW_DAGS_URL;
  }

  @Override
  public String getAirflowActiveDagRunsCountUrl() {
    return AIRFLOW_ACTIVE_DAG_RUNS_COUNT_URL;
  }

  @Override
  public String getAirflowDagRunsUrl() {
    return AIRFLOW_DAG_RUNS_URL;
  }

  @Override
  public String getAirflowDagRunsStatusUrl() {
    return AIRFLOW_DAG_RUNS_STATUS_URL;
  }

  @Override
  public String getFileShareName(FileShareConfig fileShareConfig) {
    String name = fileShareConfig.getAirflow3ShareName();
    if (name == null || name.isEmpty()) {
      LOGGER.warn(
          "fileShareConfig.airflow3ShareName not set; falling back to airflow2ShareName for "
              + "airflow3 DAG storage");
      return fileShareConfig.getAirflow2ShareName();
    }
    return name;
  }

  @Override
  public TriggerWorkflowResponse extractTriggerWorkflowResponse(String response)
      throws JsonProcessingException {
    JsonNode jsonNode = objectMapper.readValue(response, JsonNode.class);
    String logicalDate =
        jsonNode.has(LOGICAL_DATE_PARAMETER_NAME)
            ? jsonNode.get(LOGICAL_DATE_PARAMETER_NAME).asText()
            : "";
    String dagRunId =
        jsonNode.has(RUN_ID_PARAMETER_NAME) ? jsonNode.get(RUN_ID_PARAMETER_NAME).asText() : "";
    return new TriggerWorkflowResponse(logicalDate, "", dagRunId);
  }

  @Override
  public Integer extractActiveDagRunsResponse(String response) throws JsonProcessingException {
    JsonNode jsonNode = objectMapper.readValue(response, JsonNode.class);
    return jsonNode.has(ACTIVE_DAG_RUNS) ? jsonNode.get(ACTIVE_DAG_RUNS).asInt() : -1;
  }

  @Override
  public String getDagRunIdentificationParam(WorkflowEngineRequest rq) {
    return rq.getRunId();
  }

  @Override
  public JSONObject addMicroSecParam(JSONObject requestBody) {
    if (!requestBody.has(LOGICAL_DATE_PARAMETER_NAME)) {
      requestBody.put(LOGICAL_DATE_PARAMETER_NAME, Instant.now().toString());
    }
    return requestBody;
  }
}
