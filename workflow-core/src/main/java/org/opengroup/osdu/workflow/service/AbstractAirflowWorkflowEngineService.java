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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.ws.rs.HttpMethod;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.*;
import org.opengroup.osdu.workflow.model.AirflowGetDAGRunStatus;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractAirflowWorkflowEngineService implements IWorkflowEngineService {

  protected static final String AIRFLOW_EXECUTION_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
  protected static final String AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE =
      "Failed to trigger workflow with id %s and name %s";
  protected static final String AIRFLOW_WORKFLOW_RUN_NOT_FOUND =
      "No WorkflowRun executed for Workflow: %s on %s ";

  @Getter(AccessLevel.PROTECTED)
  private final IAirflowApiClient airflowApiClient;

  @Override
  public void createWorkflow(
      WorkflowEngineRequest rq, Map<String, Object> registrationInstruction) {
    // This is not relevant for a default implementation
  }

  @Override
  public void deleteWorkflow(WorkflowEngineRequest rq) {
    // This is not relevant for a default implementation
  }

  @Override
  public void saveCustomOperator(String customOperatorDefinition, String fileName) {
    // Do nothing
  }

  @Override
  public TriggerWorkflowResponse triggerWorkflow(
      WorkflowEngineRequest rq, Map<String, Object> context) {
    log.info("Submitting ingestion with dagName: {}", rq.getDagName());
    ClientResponse airflowResponse =
        airflowApiClient.callAirflow(
            HttpMethod.POST,
            getTriggerWorkflowUrl(rq),
            getTriggerWorkflowRequestBody(rq, context).toString(),
            rq,
            getTriggerWorkflowErrorMessage(rq));
    return getTriggerWorkflowResponse(airflowResponse);
  }

  protected abstract JSONObject getTriggerWorkflowRequestBody(
      WorkflowEngineRequest rq, Map<String, Object> context);

  protected abstract String getTriggerWorkflowUrl(WorkflowEngineRequest rq);

  protected String getTriggerWorkflowErrorMessage(WorkflowEngineRequest rq) {
    return format(AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE, rq.getWorkflowId(), rq.getWorkflowName());
  }

  protected abstract TriggerWorkflowResponse getTriggerWorkflowResponse(
      ClientResponse airflowResponse);

  @Override
  public WorkflowStatusType getWorkflowRunStatus(WorkflowEngineRequest rq) {
    log.info(
        rq.getWorkflowName(),
        rq.getExecutionTimeStamp());
    final ClientResponse response =
        airflowApiClient.callAirflow(
            HttpMethod.GET,
            getWorkflowRunStatusUrl(rq),
            null,
            rq,
            getWorkflowRunStatusErrorMessage(rq));

    AirflowGetDAGRunStatus airflowResponse = getAirflowGetDAGRunStatus(response);
    return airflowResponse.getStatusType();
  }

  protected abstract String getWorkflowRunStatusUrl(WorkflowEngineRequest rq);

  protected String getWorkflowRunStatusErrorMessage(WorkflowEngineRequest rq) {
    final String executionDate = executionDate(rq.getExecutionTimeStamp());
    return String.format(AIRFLOW_WORKFLOW_RUN_NOT_FOUND, rq.getWorkflowName(), executionDate);
  }

  protected AirflowGetDAGRunStatus getAirflowGetDAGRunStatus(ClientResponse response) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return objectMapper.readValue(
          response.getResponseBody().toString(), AirflowGetDAGRunStatus.class);
    } catch (JsonProcessingException e) {
      final String errorMessage = format("Unable to Process Json Received. %s", e.getMessage());
      log.error(errorMessage, e);
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Failed to Get Status from Airflow",
          errorMessage);
    }
  }

  protected String executionDate(final Long executionTimeStamp) {
    Instant instant = Instant.ofEpochMilli(executionTimeStamp);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
    return zonedDateTime.format(DateTimeFormatter.ofPattern(AIRFLOW_EXECUTION_DATE_FORMAT));
  }
}
