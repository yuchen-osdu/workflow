package org.opengroup.osdu.workflow.provider.ibm.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.*;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static java.lang.String.format;


//@Service
//@Primary
@Slf4j
public class WorkflowEngineServiceImpl implements IWorkflowEngineService {
  private static final String RUN_ID_PARAMETER_NAME = "run_id";
  private static final String AIRFLOW_EXECUTION_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
  private static final String AIRFLOW_PAYLOAD_PARAMETER_NAME = "conf";
  private final static String AIRFLOW_MICROSECONDS_FLAG = "replace_microseconds";
  private static final String TRIGGER_AIRFLOW_ENDPOINT = "api/experimental/dags/%s/dag_runs";
  private static final String AIRFLOW_RUN_ENDPOINT = "api/experimental/dags/%s/dag_runs/%s";
  private static final String EXECUTION_DATE_PARAMETER_NAME = "execution_date";


  private static final String AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE =
      "Failed to trigger workflow with id %s and name %s";
  private static final String AIRFLOW_WORKFLOW_RUN_NOT_FOUND =
      "No WorkflowRun executed for Workflow: %s on %s ";

  private final Client restClient;
  private final AirflowConfig airflowConfig;

  public WorkflowEngineServiceImpl(Client restClient, AirflowConfig airflowConfig){
    this.restClient = restClient;
    this.airflowConfig = airflowConfig;
  }

  @Override
  public void createWorkflow(final WorkflowEngineRequest rq, final Map<String, Object> registrationInstruction) {
    // This is not relevant for a default implementation
  }

  @Override
  public void deleteWorkflow(final WorkflowEngineRequest rq) {
    // This is not relevant for a default implementation
  }

  @Override
  public void saveCustomOperator(String customOperatorDefinition, String fileName) {
    //
  }

  @Override
  public TriggerWorkflowResponse triggerWorkflow(WorkflowEngineRequest rq, Map<String, Object> context) {
    log.info("Submitting ingestion with Airflow with dagName: {}", rq.getDagName());
    final String url = format(TRIGGER_AIRFLOW_ENDPOINT, rq.getDagName());
    final JSONObject requestBody = new JSONObject();
    requestBody.put(RUN_ID_PARAMETER_NAME, rq.getRunId());
    requestBody.put(AIRFLOW_PAYLOAD_PARAMETER_NAME, context);
    requestBody.put(AIRFLOW_MICROSECONDS_FLAG, "false");

    requestBody.put(EXECUTION_DATE_PARAMETER_NAME, executionDate(rq.getExecutionTimeStamp()));
    
    final String errMsg = format(AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE, rq.getWorkflowId(), rq.getWorkflowName());
    ClientResponse airflowRs = callAirflow(
        HttpMethod.POST,
        url,
        requestBody.toString(),
        rq,
        errMsg
    );
    try {
      ObjectMapper om = new ObjectMapper();
      String body = airflowRs.getResponseBody().toString();
      return om.readValue(body, TriggerWorkflowResponse.class);
    } catch (JsonProcessingException e) {
      log.info("Airflow response: {}.", airflowRs);
      final String error = "Unable to Process(Parse, Generate) JSON value";
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), error, e.getMessage());
    }
  }

  @Override
  public WorkflowStatusType getWorkflowRunStatus(WorkflowEngineRequest rq) {
    log.info("getting status of WorkflowRun of Workflow {} executed on {}", rq.getWorkflowName(),
        rq.getExecutionTimeStamp());
    //final String executionDate = executionDate(rq.getExecutionTimeStamp());
    final String executionDate = rq.getWorkflowEngineExecutionDate();

    final String url = format(AIRFLOW_RUN_ENDPOINT, rq.getWorkflowName(), executionDate);
    final String errMsg = String.format(AIRFLOW_WORKFLOW_RUN_NOT_FOUND, rq.getWorkflowName(), executionDate);
    final ClientResponse response = callAirflow(
        HttpMethod.GET,
        url,
        null,
        rq,
        errMsg);
    try {
      final ObjectMapper objectMapper = new ObjectMapper();
      final AirflowGetDAGRunStatus airflowResponse =
          objectMapper.readValue(response.getResponseBody().toString(),
              AirflowGetDAGRunStatus.class);
      return airflowResponse.getStatusType();
    } catch (JsonProcessingException e) {
      final String errorMessage = format("Unable to Process Json Received. %s", e.getMessage());
      log.error(errorMessage, e);
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to Get Status from Airflow", errorMessage);
    }
  }

  protected ClientResponse callAirflow(String httpMethod, String apiEndpoint, String body,
      WorkflowEngineRequest rq, String errorMessage) {
    String url = format("%s/%s", airflowConfig.getUrl(), apiEndpoint);
    log.info("Calling airflow endpoint {} with method {}", url, httpMethod);

    WebResource webResource = restClient.resource(url);
    com.sun.jersey.api.client.ClientResponse response = webResource
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Basic " + airflowConfig.getAppKey())
        .method(httpMethod, com.sun.jersey.api.client.ClientResponse.class, body);

    final int status = response.getStatus();
    log.info("Received response status: {}.", status);

    if (status != HttpStatus.OK.value()) {
      String responseBody = response.getEntity(String.class);
      throw new AppException(status, responseBody, errorMessage);
    }

    return ClientResponse.builder()
        .contentType(String.valueOf(response.getType()))
        .responseBody(response.getEntity(String.class))
        .status(HttpStatus.OK)
        .statusCode(response.getStatus())
        .statusMessage(response.getStatusInfo().getReasonPhrase())
        .build();
  }

  protected String executionDate(final Long executionTimeStamp){
    Instant instant = Instant.ofEpochMilli(executionTimeStamp);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
    return zonedDateTime.format(DateTimeFormatter.ofPattern(AIRFLOW_EXECUTION_DATE_FORMAT));
  }
}

