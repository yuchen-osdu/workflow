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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;

@Slf4j
public class AirflowWorkflowEngineServiceImpl extends AbstractAirflowWorkflowEngineService {
  protected static final String RUN_ID_PARAMETER_NAME = "run_id";
  protected static final String AIRFLOW_PAYLOAD_PARAMETER_NAME = "conf";
  protected static final String EXECUTION_DATE_PARAMETER_NAME = "execution_date";
  protected static final String TRIGGER_AIRFLOW_ENDPOINT = "api/experimental/dags/%s/dag_runs";
  protected static final String AIRFLOW_RUN_ENDPOINT = "api/experimental/dags/%s/dag_runs/%s";

  public AirflowWorkflowEngineServiceImpl(IAirflowApiClient airflowApiClient) {
    super(airflowApiClient);
  }

  @Override
  protected JSONObject getTriggerWorkflowRequestBody(
      WorkflowEngineRequest rq, Map<String, Object> context) {
    final JSONObject requestBody = new JSONObject();
    requestBody.put(RUN_ID_PARAMETER_NAME, rq.getRunId());
    requestBody.put(AIRFLOW_PAYLOAD_PARAMETER_NAME, context);
    requestBody.put(EXECUTION_DATE_PARAMETER_NAME, executionDate(rq.getExecutionTimeStamp()));
    return requestBody;
  }

  @Override
  protected String getTriggerWorkflowUrl(WorkflowEngineRequest rq) {
    return format(TRIGGER_AIRFLOW_ENDPOINT, rq.getDagName());
  }

  @Override
  protected TriggerWorkflowResponse getTriggerWorkflowResponse(ClientResponse airflowResponse) {
    try {
      ObjectMapper om = new ObjectMapper();
      String body = airflowResponse.getResponseBody().toString();
      return om.readValue(body, TriggerWorkflowResponse.class);
    } catch (JsonProcessingException e) {
      log.info("Airflow response: {}.", airflowResponse);
      final String error = "Unable to Process(Parse, Generate) JSON value";
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), error, e.getMessage());
    }
  }

  @Override
  protected String getWorkflowRunStatusUrl(WorkflowEngineRequest rq) {
    final String executionDate = executionDate(rq.getExecutionTimeStamp());
    return format(AIRFLOW_RUN_ENDPOINT, rq.getDagName(), executionDate);
  }
}
