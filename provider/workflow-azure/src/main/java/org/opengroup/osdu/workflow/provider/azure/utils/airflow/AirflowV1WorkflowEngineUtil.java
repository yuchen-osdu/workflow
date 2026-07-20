package org.opengroup.osdu.workflow.provider.azure.utils.airflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "osdu.airflow.version2.enabled", havingValue = "false", matchIfMissing = true)
public class AirflowV1WorkflowEngineUtil implements IAirflowWorkflowEngineUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(AirflowV1WorkflowEngineUtil.class);
  private static final String FILE_NAME_PREFIX = ".py";
  private static final String AIRFLOW_DAGS_URL = "api/experimental/dags/%s";
  private static final String AIRFLOW_DAG_RUNS_URL = "api/experimental/dags/%s/dag_runs";
  private static final String AIRFLOW_DAG_RUNS_STATUS_URL = "api/experimental/dags/%s/dag_runs/%s";
  private final static String RUN_ID_PARAMETER_NAME = "run_id";
  private final static String AIRFLOW_MICROSECONDS_FLAG = "replace_microseconds";
  private static final String AIRFLOW_ACTIVE_DAG_RUNS_COUNT_URL = "activeDagRuns/";
  private final static String ACTIVE_DAG_RUNS = "active_dag_runs";

  @Autowired
  @Qualifier("WorkflowObjectMapper")
  private ObjectMapper OBJECT_MAPPER;

  public String getDagRunIdParameterName() {
    return RUN_ID_PARAMETER_NAME;
  }

  public String getFileNameFromWorkflow(String workflowName) {
    return workflowName + FILE_NAME_PREFIX;
  }

  public String getAirflowDagsUrl() {
    return AIRFLOW_DAGS_URL;
  }

  public String getAirflowDagRunsUrl() {
    return AIRFLOW_DAG_RUNS_URL;
  }

  public String getAirflowDagRunsStatusUrl() {
    return AIRFLOW_DAG_RUNS_STATUS_URL;
  }

  public String getAirflowActiveDagRunsCountUrl() {
    return AIRFLOW_ACTIVE_DAG_RUNS_COUNT_URL;
  }

  public String getFileShareName(FileShareConfig fileShareConfig) {
    return fileShareConfig.getShareName();
  }

  public TriggerWorkflowResponse extractTriggerWorkflowResponse(String response)
      throws JsonProcessingException {
    return OBJECT_MAPPER.readValue(response, TriggerWorkflowResponse.class);
  }

  public Integer extractActiveDagRunsResponse(String response)
      throws JsonProcessingException {
    JsonNode jsonNode = OBJECT_MAPPER.readValue(response, JsonNode.class);
    Integer activeDagRuns = jsonNode.has(ACTIVE_DAG_RUNS)
        ? jsonNode.get(ACTIVE_DAG_RUNS).asInt()
        : -1;
    return activeDagRuns;
  }

  public String getDagRunIdentificationParam(WorkflowEngineRequest rq) {
    return rq.getWorkflowEngineExecutionDate();
  }


  public JSONObject addMicroSecParam(JSONObject requestBody) {
    requestBody.put(AIRFLOW_MICROSECONDS_FLAG, "false");
    return requestBody;
  }


}
