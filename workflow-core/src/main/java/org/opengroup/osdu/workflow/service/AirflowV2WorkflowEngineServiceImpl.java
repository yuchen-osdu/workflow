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

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;

@Slf4j
public class AirflowV2WorkflowEngineServiceImpl extends AbstractAirflowWorkflowEngineService {

  protected static final String RUN_ID_PARAMETER_NAME_STABLE = "dag_run_id";
  protected static final String AIRFLOW_PAYLOAD_PARAMETER_NAME = "conf";
  protected static final String EXECUTION_DATE_PARAMETER_NAME = "execution_date";
  protected static final String TRIGGER_AIRFLOW_ENDPOINT_STABLE = "api/v1/dags/%s/dagRuns";
  protected static final String AIRFLOW_RUN_ENDPOINT_STABLE = "api/v1/dags/%s/dagRuns/%s";
  protected static final String AIRFLOW_VERSION_ENDPOINT = "api/v1/version";
  protected static final String NOT_AVAILABLE = "N/A";
  protected static final String VERSION = "version";
  protected static final String KEY_USER_ID = "userId";
  protected static final String KEY_EXECUTION_CONTEXT = "execution_context";

  private final DpsHeaders dpsHeaders;

  public AirflowV2WorkflowEngineServiceImpl(
      IAirflowApiClient airflowApiClient, DpsHeaders dpsHeaders) {
    super(airflowApiClient);
    this.dpsHeaders = dpsHeaders;
  }

  @Override
  protected JSONObject getTriggerWorkflowRequestBody(
      WorkflowEngineRequest rq, Map<String, Object> context) {
    final JSONObject requestBody = new JSONObject();
    requestBody.put(AIRFLOW_PAYLOAD_PARAMETER_NAME, context);
    requestBody.put(RUN_ID_PARAMETER_NAME_STABLE, rq.getRunId());
    return requestBody;
  }

  @Override
  protected String getTriggerWorkflowUrl(WorkflowEngineRequest rq) {
    return format(TRIGGER_AIRFLOW_ENDPOINT_STABLE, rq.getDagName());
  }

  @Override
  protected TriggerWorkflowResponse getTriggerWorkflowResponse(ClientResponse airflowResponse) {
    try {
      ObjectMapper om = new ObjectMapper();
      String body = airflowResponse.getResponseBody().toString();
      JsonNode jsonNode = om.readValue(body, JsonNode.class);
      String executionDate = "";
      String dagRunId = "";
      if (jsonNode.has(EXECUTION_DATE_PARAMETER_NAME))
        executionDate = jsonNode.get(EXECUTION_DATE_PARAMETER_NAME).asText();
      if (jsonNode.has(RUN_ID_PARAMETER_NAME_STABLE))
        dagRunId = jsonNode.get(RUN_ID_PARAMETER_NAME_STABLE).asText();

      return new TriggerWorkflowResponse(executionDate, "", dagRunId);

    } catch (JsonProcessingException e) {
      log.info("Airflow response: {}.", airflowResponse);
      final String error = "Unable to Process(Parse, Generate) JSON value";
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), error, e.getMessage());
    }
  }

  @Override
  protected String getWorkflowRunStatusUrl(WorkflowEngineRequest rq) {
    return format(AIRFLOW_RUN_ENDPOINT_STABLE, rq.getDagName(), rq.getRunId());
  }

  @Override
  public TriggerWorkflowResponse triggerWorkflow(
      WorkflowEngineRequest rq, Map<String, Object> context) {
    addUserIdToExecutionContext(context, rq);
    return super.triggerWorkflow(rq, context);
  }

  @Override
  public Optional<String> getVersion() {
    ClientResponse clientResponse =
        getAirflowApiClient().callAirflow(HttpMethod.GET, AIRFLOW_VERSION_ENDPOINT, null, null, null);
    try {
      ObjectMapper om = new ObjectMapper();
      String body = clientResponse.getResponseBody().toString();
      JsonNode jsonNode = om.readValue(body, JsonNode.class);
      if (jsonNode.has(VERSION)) {
        return Optional.of(jsonNode.get(VERSION).asText());
      } else {
        log.error(
            "Unable to locate version in Airflow response. Airflow response: {}.", clientResponse);
        return Optional.of(NOT_AVAILABLE);
      }
    } catch (JsonProcessingException e) {
      log.error(
          "Unable to Process(Parse, Generate) JSON value. Airflow response: {}.", clientResponse);
      return Optional.of(NOT_AVAILABLE);
    }
  }

  protected void addUserIdToExecutionContext(
      Map<String, Object> inputData, WorkflowEngineRequest rq) {
    if (Objects.isNull(inputData)) {
      throw new AppException(
          HttpStatus.BAD_REQUEST.value(),
          "Failed to trigger workflow run",
          "data is null or empty");
    }
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> executionContext =
        objectMapper.convertValue(inputData.get(KEY_EXECUTION_CONTEXT), Map.class);
    if (Objects.isNull(executionContext)) {
      throw new AppException(
          HttpStatus.BAD_REQUEST.value(),
          "Failed to trigger workflow run",
          "execution_context is null or empty");
    }
    if (executionContext.containsKey(KEY_USER_ID)) {
      String errorMessage =
          String.format(
              "Request to trigger workflow with name %s failed because execution context contains reserved key 'userId'",
              rq.getWorkflowName());
      throw new AppException(400, "Failed to trigger workflow run", errorMessage);
    }
    log.debug("putting user email: {} in execution context", dpsHeaders.getUserEmail());
    executionContext.put(KEY_USER_ID, dpsHeaders.getUserEmail());
    inputData.put(KEY_EXECUTION_CONTEXT, executionContext);
  }
}

