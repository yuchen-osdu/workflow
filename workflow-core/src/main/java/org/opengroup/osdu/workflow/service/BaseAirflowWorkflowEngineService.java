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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;

/**
 * Shared base for the Airflow 2 ({@code api/v1}) and Airflow 3 ({@code api/v2}) engine
 * implementations. Holds the logic that is common to both versions — OSDU user injection into the
 * execution context, the {@code conf}/{@code dag_run_id} payload naming, and version parsing — so
 * the two concrete engines are independent siblings. Version-specific bits (endpoint layout and the
 * trigger request/response date field) are provided by the subclasses.
 *
 * <p>Because Airflow 2 and Airflow 3 do not inherit from one another, either engine can be removed
 * without impacting the other (e.g. an Airflow 2 decommission).
 */
@Slf4j
public abstract class BaseAirflowWorkflowEngineService extends AbstractAirflowWorkflowEngineService {

  protected static final String AIRFLOW_PAYLOAD_PARAMETER_NAME = "conf";
  protected static final String RUN_ID_PARAMETER_NAME = "dag_run_id";
  protected static final String VERSION = "version";
  protected static final String NOT_AVAILABLE = "N/A";
  protected static final String KEY_USER_ID = "userId";
  protected static final String KEY_EXECUTION_CONTEXT = "execution_context";

  private final DpsHeaders dpsHeaders;

  protected BaseAirflowWorkflowEngineService(
      IAirflowApiClient airflowApiClient, DpsHeaders dpsHeaders) {
    super(airflowApiClient);
    this.dpsHeaders = dpsHeaders;
  }

  /** Endpoint returning the Airflow version for this engine (e.g. {@code api/v1/version}). */
  protected abstract String getVersionEndpoint();

  @Override
  public TriggerWorkflowResponse triggerWorkflow(
      WorkflowEngineRequest rq, Map<String, Object> context) {
    addUserIdToExecutionContext(context, rq);
    return super.triggerWorkflow(rq, context);
  }

  @Override
  public Optional<String> getVersion() {
    ClientResponse clientResponse;
    try {
      clientResponse =
          getAirflowApiClient().callAirflow(HttpMethod.GET, getVersionEndpoint(), null, null, null);
    } catch (AppException e) {
      log.error("Unable to retrieve Airflow version.", e);
      return Optional.of(NOT_AVAILABLE);
    }
    try {
      ObjectMapper om = new ObjectMapper();
      String body = clientResponse.getResponseBody().toString();
      JsonNode jsonNode = om.readValue(body, JsonNode.class);
      if (jsonNode.has(VERSION)) {
        return Optional.of(jsonNode.get(VERSION).asText());
      }
      log.error(
          "Unable to locate version in Airflow response. Airflow response: {}.", clientResponse);
      return Optional.of(NOT_AVAILABLE);
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
