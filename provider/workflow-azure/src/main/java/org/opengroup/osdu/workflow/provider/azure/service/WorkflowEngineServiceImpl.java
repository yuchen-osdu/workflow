package org.opengroup.osdu.workflow.provider.azure.service;

import com.azure.storage.file.share.models.ShareStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.AirflowGetDAGRunStatus;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.provider.azure.config.ActiveDagRunsConfig;
import org.opengroup.osdu.workflow.provider.azure.config.AirflowConfigResolver;
import org.opengroup.osdu.workflow.provider.azure.config.AzureWorkflowEngineConfig;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareConfig;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareStore;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IActiveDagRunsCache;
import org.opengroup.osdu.workflow.provider.azure.utils.airflow.IAirflowWorkflowEngineUtil;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.opengroup.osdu.workflow.provider.azure.consts.CacheConstants.ACTIVE_DAG_RUNS_COUNT_CACHE_KEY;

@Slf4j
@Service
@Primary
public class WorkflowEngineServiceImpl implements IWorkflowEngineService {
  private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowEngineServiceImpl.class);
  private static final String AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE = "Failed to trigger workflow with id %s and name %s";
  private static final String AIRFLOW_DELETE_DAG_ERROR_MESSAGE = "Failed to delete workflow with name %s";
  private static final String AIRFLOW_GET_ACTIVE_DAG_RUNS_ERROR_MESSAGE = "Failed to get all active dag runs";
  private static final String AIRFLOW_WORKFLOW_RUN_NOT_FOUND = "No WorkflowRun executed for Workflow: %s on %s ";
  private final static String AIRFLOW_PAYLOAD_PARAMETER_NAME = "conf";

  private final static String AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_TRIGGER_CONFIGURATION = "_trigger_config";
  private final static String AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_WORKFLOW_ID = "trigger_dag_id";
  private final static String AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_WORKFLOW_RUN_ID = "trigger_dag_run_id";
  private static final String KEY_DAG_CONTENT = "dagContent";
  private static final String KEY_USER_ID = "userId";
  private static final String KEY_EXECUTION_CONTEXT = "execution_context";


  @Autowired
  private AirflowConfigResolver airflowConfigResolver;

  @Autowired
  private Client restClient;

  @Autowired
  private FileShareConfig fileShareConfig;

  @Autowired
  @Qualifier("IngestFileShareStore")
  private FileShareStore fileShareStore;

  @Autowired
  private DpsHeaders dpsHeaders;

  @Autowired
  private AzureWorkflowEngineConfig workflowEngineConfig;

  @Autowired
  private IAirflowWorkflowEngineUtil engineUtil;

  @Autowired
  @Qualifier("WorkflowObjectMapper")
  private ObjectMapper om;

  @Autowired
  @Qualifier("ActiveDagRunsCache")
  private IActiveDagRunsCache<String, Integer> activeDagRunsCache;

  @Autowired
  private ActiveDagRunsConfig activeDagRunsConfig;

  @Autowired
  private PartitionServiceClient partitionService;

  @Override
  public void createWorkflow(
      final WorkflowEngineRequest rq, final Map<String, Object> registrationInstruction) {
    String dagContent = (String) registrationInstruction.get(KEY_DAG_CONTENT);
    if (workflowEngineConfig.getIgnoreDagContent()) {
      LOGGER.info("Ignoring input DAG content: {}", dagContent);
      dagContent = "";
    }
    if (dagContent != null && !dagContent.isEmpty()) {
      if (!rq.isSystemWorkflow()) {
        fileShareStore.writeToFileShare(dpsHeaders.getPartitionId(), engineUtil.getFileShareName(fileShareConfig),
            fileShareConfig.getDagsFolder(), engineUtil.getFileNameFromWorkflow(rq.getWorkflowName()),
            dagContent);
      } else {
        fileShareStore.writeToFileShare(engineUtil.getFileShareName(fileShareConfig),
            fileShareConfig.getDagsFolder(), engineUtil.getFileNameFromWorkflow(rq.getWorkflowName()),
            dagContent);
      }
    }
  }

  @Override
  public void deleteWorkflow(WorkflowEngineRequest rq) {
    String workflowName = rq.getWorkflowName();
    LOGGER.info("Deleting DAG {} in Airflow", workflowName);

    if (rq.isDeployedThroughWorkflowService()) {
      // Deleting only if dag is deployed through workflow service.
      // Figure out how to only remove the metadata but not the DAG.
      // Because in repeated delete create fashion the dag will not appear for a while
      try {
        String deleteDAGEndpoint = String.format(engineUtil.getAirflowDagsUrl(), workflowName);

        callAirflowApi(getAirflowConfig(rq.isSystemWorkflow()), deleteDAGEndpoint, HttpMethod.DELETE,
            null, String.format(AIRFLOW_DELETE_DAG_ERROR_MESSAGE, workflowName));
      } catch (AppException e) {
        if (e.getError().getCode() != 404) {
          throw e;
        }
      }

      String fileName = engineUtil.getFileNameFromWorkflow(workflowName);
      LOGGER.info("Deleting DAG file {} from file share", fileName);
      try {
        if (!rq.isSystemWorkflow()) {
          fileShareStore.deleteFromFileShare(dpsHeaders.getPartitionId(),
              engineUtil.getFileShareName(fileShareConfig), fileShareConfig.getDagsFolder(),
              fileName);
        } else {
          fileShareStore.deleteFromFileShare(engineUtil.getFileShareName(fileShareConfig),
              fileShareConfig.getDagsFolder(), fileName);
        }
      } catch (final ShareStorageException e) {
        if (e.getStatusCode() != 404) {
          throw e;
        }
      }
    }
  }

  @Override
  public void saveCustomOperator(final String customOperatorDefinition, final String fileName) {
    fileShareStore.writeToFileShare(dpsHeaders.getPartitionId(),
        engineUtil.getFileShareName(fileShareConfig),
        fileShareConfig.getCustomOperatorsFolder(), fileName, customOperatorDefinition);
  }

  private ClientResponse triggerWorkflowBase(AirflowConfig airflowConfig, final String runId,
                                             final String workflowId, String workflowName,
                                             final Map<String, Object> inputData) {
    String triggerDAGEndpoint = String.format(engineUtil.getAirflowDagRunsUrl(), workflowName);

    JSONObject requestBody = new JSONObject();
    requestBody.put(engineUtil.getDagRunIdParameterName(), runId);
    requestBody.put(AIRFLOW_PAYLOAD_PARAMETER_NAME, inputData);
    requestBody = engineUtil.addMicroSecParam(requestBody);

    return callAirflowApi(airflowConfig, triggerDAGEndpoint, HttpMethod.POST,
        requestBody.toString(),
        String.format(AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE, workflowId, workflowName));
  }

  private ClientResponse triggerWorkflowUsingController(
      AirflowConfig airflowConfig, final String runId, final String workflowId,
      String workflowName, Map<String, Object> inputData, boolean isSystemWorkflow) {
    String controllerId = getAirflowConfig(isSystemWorkflow).getControllerDagId();
    String triggerDAGEndpoint = String.format(engineUtil.getAirflowDagRunsUrl(), controllerId);

    JSONObject requestBody = new JSONObject();
    String parentRunId = "PARENT_" + runId;
    requestBody.put(engineUtil.getDagRunIdParameterName(), parentRunId);

    Map<String, String> triggerParams = new HashMap<>();
    triggerParams.put(AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_WORKFLOW_ID, workflowName);
    triggerParams.put(AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_WORKFLOW_RUN_ID, runId);

    inputData.put(AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_TRIGGER_CONFIGURATION, triggerParams);
    requestBody.put(AIRFLOW_PAYLOAD_PARAMETER_NAME, inputData);
    requestBody = engineUtil.addMicroSecParam(requestBody);

    return callAirflowApi(airflowConfig, triggerDAGEndpoint, HttpMethod.POST,
        requestBody.toString(),
        String.format(AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE, workflowId, workflowName));
  }

  @Override
  public TriggerWorkflowResponse triggerWorkflow(WorkflowEngineRequest rq,
                                                 Map<String, Object> inputData) {

    PartitionInfoAzure pi = this.partitionService.getPartition(dpsHeaders.getPartitionId());
    Boolean isAirflowEnabled = pi.getAirflowEnabled();
    // NOTE: [aaljain] limiting trigger requests not supported for multi partition
    if (!isAirflowEnabled) {
      checkAndUpdateActiveDagRunsCache();
    }
    String workflowName = rq.getWorkflowName();
    String runId = rq.getRunId();
    String workflowId = rq.getWorkflowId();
    LOGGER.info("Submitting ingestion with Airflow with dagName: {}", workflowName);
    ClientResponse response;
    AirflowConfig airflowConfig = getAirflowConfig(rq.isSystemWorkflow());
    addUserIdToExecutionContext(inputData, rq);
    if (airflowConfig.isDagRunAbstractionEnabled()) {
      response = triggerWorkflowUsingController(airflowConfig, runId, workflowId,
          workflowName, inputData, rq.isSystemWorkflow());
    } else {
      response = triggerWorkflowBase(airflowConfig, runId, workflowId, workflowName, inputData);
    }

    try {
      final TriggerWorkflowResponse triggerWorkflowResponse = engineUtil.
          extractTriggerWorkflowResponse(response.getEntity(String.class));
      LOGGER.info("Airflow response: {}.", triggerWorkflowResponse);
      if (!isAirflowEnabled) {
        incrementActiveDagRunsCountInCache();
      }
      return triggerWorkflowResponse;
    } catch (JsonProcessingException e) {
      final String error = "Unable to Process(Parse, Generate) JSON value";
      throw new AppException(500, error, e.getMessage());
    }
  }

  private void checkAndUpdateActiveDagRunsCache() {
    Integer numberOfActiveDagRuns = activeDagRunsCache.get(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY);
    if (numberOfActiveDagRuns == null) {
      LOGGER.info("Obtaining number of active dag runs from airflow postgresql db");
      try {
        numberOfActiveDagRuns = getActiveDagRunsCount();
      } catch (Exception e) {
        LOGGER.error("Unable to obtain active dag runs count from airflow database", e);
      }
    }

    if (numberOfActiveDagRuns != null) {
      if (numberOfActiveDagRuns >= activeDagRunsConfig.getThreshold()) {
        throw new AppException(HttpStatus.TOO_MANY_REQUESTS.value(), "Triggering a new dag run is not allowed", "Maximum threshold for number of active dag runs reached");
      }
      activeDagRunsCache.put(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY, numberOfActiveDagRuns);
      LOGGER.info("Number of active dag runs present: {}", numberOfActiveDagRuns);
    }
  }

  private void incrementActiveDagRunsCountInCache() {
    Integer numberOfActiveDagRuns = activeDagRunsCache.get(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY);
    if (numberOfActiveDagRuns != null) {
      LOGGER.info("Incrementing the number of active dag runs in cache to {}", numberOfActiveDagRuns + 1);
      activeDagRunsCache.incrementKey(ACTIVE_DAG_RUNS_COUNT_CACHE_KEY);
    }
  }

  private ClientResponse callAirflowApi(AirflowConfig airflowConfig, String apiEndpoint,
                                        String method, Object body, String errorMessage) {
    String url = String.format("%s/%s", airflowConfig.getUrl(), apiEndpoint);
    LOGGER.info("Calling airflow endpoint {} with method {}", url, method);

    WebResource webResource = restClient.resource(url);
    ClientResponse response = webResource
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Basic " + airflowConfig.getAppKey())
        .method(method, ClientResponse.class, body);

    final int status = response.getStatus();
    LOGGER.info("Received response status: {}.", status);

    if (status != 200) {
      String responseBody = response.getEntity(String.class);
      throw new AppException(status, responseBody, errorMessage);
    }
    return response;
  }

  @Override
  public WorkflowStatusType getWorkflowRunStatus(WorkflowEngineRequest rq) {
    String workflowName = rq.getWorkflowName();
    String dagRunIdentificationParam = engineUtil.getDagRunIdentificationParam(rq);
    LOGGER.info("getting status of WorkflowRun of Workflow {} with identification on {}",
        workflowName, dagRunIdentificationParam);
    String getDAGRunStatusEndpoint = String.format(engineUtil.getAirflowDagRunsStatusUrl(),
        workflowName, dagRunIdentificationParam);
    ClientResponse response = callAirflowApi(getAirflowConfig(rq.isSystemWorkflow()),
        getDAGRunStatusEndpoint, HttpMethod.GET, null,
        String.format(AIRFLOW_WORKFLOW_RUN_NOT_FOUND, workflowName, dagRunIdentificationParam));
    try {
      final AirflowGetDAGRunStatus airflowResponse = om.readValue(response.getEntity(String.class),
          AirflowGetDAGRunStatus.class);
      return airflowResponse.getStatusType();
    } catch (JsonProcessingException e) {
      String errorMessage = "Unable to Process Json Received. " + e.getMessage();
      LOGGER.error(errorMessage + e.getStackTrace());
      throw new AppException(500, "Failed to Get Status from Airflow", errorMessage);
    }
  }


  private AirflowConfig getAirflowConfig(Boolean isSystemWorkflow) {
    if (isSystemWorkflow) {
      if (workflowEngineConfig.getIsDPAirflowUsedForSystemDAG()) {
        return airflowConfigResolver.getAirflowConfig(dpsHeaders.getPartitionId());
      } else {
        return airflowConfigResolver.getSystemAirflowConfig();
      }
    } else {
      return airflowConfigResolver.getAirflowConfig(dpsHeaders.getPartitionId());
    }
  }

  private Integer getActiveDagRunsCount() throws Exception {
    LOGGER.info("Obtaining active dag runs from Airflow");
    String endpoint = engineUtil.getAirflowActiveDagRunsCountUrl();
    ClientResponse clientResponse = callAirflowApi(getAirflowConfig(false), endpoint, HttpMethod.GET,
        null, AIRFLOW_GET_ACTIVE_DAG_RUNS_ERROR_MESSAGE);

    Integer activeDagRuns = engineUtil.extractActiveDagRunsResponse(clientResponse.getEntity(String.class));

    if (activeDagRuns != -1) {
      return activeDagRuns;
    }
    throw new Exception("Failed to retrieve active dag runs, got null response");
  }

  private void addUserIdToExecutionContext(Map<String, Object> inputData, WorkflowEngineRequest rq) {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> executionContext = objectMapper.convertValue(inputData.get(KEY_EXECUTION_CONTEXT), Map.class);
    if (executionContext.containsKey(KEY_USER_ID)) {
      String errorMessage = String.format("Request to trigger workflow with name %s failed because execution context contains reserved key 'userId'", rq.getWorkflowName());
      throw new AppException(400, "Failed to trigger workflow run", errorMessage);
    }
    log.debug(String.format("putting user id: %s in execution context",dpsHeaders.getUserId()));
    executionContext.put(KEY_USER_ID, dpsHeaders.getUserId());
    inputData.put(KEY_EXECUTION_CONTEXT,executionContext);
  }
}
