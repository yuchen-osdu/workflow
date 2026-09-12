package org.opengroup.osdu.workflow.provider.azure.service;

import com.azure.storage.file.share.models.ShareStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
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
import org.opengroup.osdu.workflow.provider.azure.utils.airflow.AirflowEngineUtilSelector;
import org.opengroup.osdu.workflow.provider.azure.utils.airflow.IAirflowWorkflowEngineUtil;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.Airflow3TokenClient;
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
  private AirflowEngineUtilSelector engineUtilSelector;

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

  /** Lazily-built JWT token client for the Airflow 3 backend (single deployment-wide host). */
  private volatile Airflow3TokenClient airflow3TokenClient;

  @Override
  public void createWorkflow(
      final WorkflowEngineRequest rq, final Map<String, Object> registrationInstruction) {
    IAirflowWorkflowEngineUtil engineUtil = engineUtilSelector.utilFor(rq);
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
    IAirflowWorkflowEngineUtil engineUtil = engineUtilSelector.utilFor(rq);
    String workflowName = rq.getWorkflowName();
    LOGGER.info("Deleting DAG {} in Airflow", workflowName);

    if (rq.isDeployedThroughWorkflowService()) {
      // Deleting only if dag is deployed through workflow service.
      // Figure out how to only remove the metadata but not the DAG.
      // Because in repeated delete create fashion the dag will not appear for a while
      try {
        String deleteDAGEndpoint = String.format(engineUtil.getAirflowDagsUrl(), workflowName);

        callAirflowApi(resolveAirflowConfig(rq), isAirflow3Run(rq),
            deleteDAGEndpoint, HttpMethod.DELETE,
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
    IAirflowWorkflowEngineUtil engineUtil = engineUtilSelector.getDefaultUtil();
    fileShareStore.writeToFileShare(dpsHeaders.getPartitionId(),
        engineUtil.getFileShareName(fileShareConfig),
        fileShareConfig.getCustomOperatorsFolder(), fileName, customOperatorDefinition);
  }

  private ClientResponse triggerWorkflowBase(AirflowConfig airflowConfig, boolean isAirflow3,
                                             IAirflowWorkflowEngineUtil engineUtil, final String runId,
                                             final String workflowId, String workflowName,
                                             final Map<String, Object> inputData) {
    String triggerDAGEndpoint = String.format(engineUtil.getAirflowDagRunsUrl(), workflowName);

    JSONObject requestBody = new JSONObject();
    requestBody.put(engineUtil.getDagRunIdParameterName(), runId);
    requestBody.put(AIRFLOW_PAYLOAD_PARAMETER_NAME, inputData);
    requestBody = engineUtil.addMicroSecParam(requestBody);

    return callAirflowApi(airflowConfig, isAirflow3, triggerDAGEndpoint, HttpMethod.POST,
        requestBody.toString(),
        String.format(AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE, workflowId, workflowName));
  }

  private ClientResponse triggerWorkflowUsingController(
      AirflowConfig airflowConfig, boolean isAirflow3, IAirflowWorkflowEngineUtil engineUtil, final String runId,
      final String workflowId, String workflowName, Map<String, Object> inputData) {
    String controllerId = airflowConfig.getControllerDagId();
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

    return callAirflowApi(airflowConfig, isAirflow3, triggerDAGEndpoint, HttpMethod.POST,
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
    IAirflowWorkflowEngineUtil engineUtil = engineUtilSelector.utilFor(rq);
    AirflowConfig airflowConfig = resolveAirflowConfig(rq);
    boolean isAirflow3 = isAirflow3Run(rq);
    addUserIdToExecutionContext(inputData, rq);
    if (airflowConfig.isDagRunAbstractionEnabled()) {
      response = triggerWorkflowUsingController(airflowConfig, isAirflow3, engineUtil, runId, workflowId,
          workflowName, inputData);
    } else {
      response = triggerWorkflowBase(airflowConfig, isAirflow3, engineUtil, runId, workflowId, workflowName,
          inputData);
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

  private ClientResponse callAirflowApi(AirflowConfig airflowConfig, boolean isAirflow3,
                                        String apiEndpoint, String method, Object body,
                                        String errorMessage) {
    String url = String.format("%s/%s", airflowConfig.getUrl(), apiEndpoint);
    LOGGER.info("Calling airflow endpoint {} with method {}", url, method);

    ClientResponse response;
    if (isAirflow3) {
      // Airflow 3 api/v2 uses native JWT bearer auth (POST /auth/token), not HTTP Basic.
      Airflow3TokenClient tokenClient = airflow3TokenClient(airflowConfig);
      String token = tokenClient.token();
      response = invokeWithBearer(url, method, body, token);
      if (isUnauthorized(response.getStatus())) {
        LOGGER.warn("Airflow 3 returned {} for {}; refreshing JWT and retrying once.",
            response.getStatus(), url);
        response.close();
        response = invokeWithBearer(url, method, body, tokenClient.refreshIfStale(token));
      }
    } else {
      response = invokeWithBasic(url, method, body, airflowConfig.getAppKey());
    }

    final int status = response.getStatus();
    LOGGER.info("Received response status: {}.", status);

    if (!isSuccess(status)) {
      String responseBody = response.getEntity(String.class);
      throw new AppException(status, responseBody, errorMessage);
    }
    return response;
  }

  private ClientResponse invokeWithBearer(String url, String method, Object body, String token) {
    return restClient.resource(url)
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token)
        .method(method, ClientResponse.class, body);
  }

  private ClientResponse invokeWithBasic(String url, String method, Object body, String appKey) {
    return restClient.resource(url)
        .type(MediaType.APPLICATION_JSON)
        .header("Authorization", "Basic " + appKey)
        .method(method, ClientResponse.class, body);
  }

  /**
   * Lazily builds a single {@link Airflow3TokenClient} for the (deployment-wide) Airflow 3 backend
   * so the JWT is cached and refreshed once per host rather than per request.
   */
  private Airflow3TokenClient airflow3TokenClient(AirflowConfig airflow3Config) {
    Airflow3TokenClient client = this.airflow3TokenClient;
    if (client == null) {
      synchronized (this) {
        client = this.airflow3TokenClient;
        if (client == null) {
          client = new Airflow3TokenClient(restClient, airflow3Config);
          this.airflow3TokenClient = client;
        }
      }
    }
    return client;
  }

  private static boolean isSuccess(int status) {
    return status >= HttpStatus.OK.value() && status < 300;
  }

  private static boolean isUnauthorized(int status) {
    return status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value();
  }

  @Override
  public WorkflowStatusType getWorkflowRunStatus(WorkflowEngineRequest rq) {
    IAirflowWorkflowEngineUtil engineUtil = engineUtilSelector.utilFor(rq);
    String workflowName = rq.getWorkflowName();
    String dagRunIdentificationParam = engineUtil.getDagRunIdentificationParam(rq);
    LOGGER.info("getting status of WorkflowRun of Workflow {} with identification on {}",
        workflowName, dagRunIdentificationParam);
    String getDAGRunStatusEndpoint = String.format(engineUtil.getAirflowDagRunsStatusUrl(),
        workflowName, dagRunIdentificationParam);
    ClientResponse response = callAirflowApi(resolveAirflowConfig(rq),
        isAirflow3Run(rq),
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

  /**
   * Resolves the Airflow backend config that owns the request. Airflow 3 runs use the single
   * deployment-wide Airflow 3 config; all other runs use the partition/system Airflow 2 config.
   */
  private AirflowConfig resolveAirflowConfig(WorkflowEngineRequest rq) {
    if (isAirflow3Run(rq)) {
      return airflowConfigResolver.getAirflow3Config();
    }
    return getAirflowConfig(rq != null && rq.isSystemWorkflow());
  }

  /**
   * A request is treated as Airflow 3 only when it is stamped {@code airflow3} AND Airflow 3 is
   * actually enabled in this deployment. If Airflow 3 has been disabled/rolled back while an AF3 run
   * is still in flight, the run degrades to the base (Airflow 2) engine/config/auth consistently —
   * Airflow then returns 404 for a run it never owned, which is a graceful failure vs a hard 500.
   */
  private boolean isAirflow3Run(WorkflowEngineRequest rq) {
    return rq != null
        && AirflowEngineVersions.isAirflow3(rq.getEngineVersion())
        && engineUtilSelector.isV3Available();
  }

  private Integer getActiveDagRunsCount() throws Exception {
    IAirflowWorkflowEngineUtil engineUtil = engineUtilSelector.getDefaultUtil();
    boolean isAirflow3 = engineUtilSelector.isV3Available();
    AirflowConfig airflowConfig =
        isAirflow3 ? airflowConfigResolver.getAirflow3Config() : getAirflowConfig(false);
    LOGGER.info("Obtaining active dag runs from Airflow");
    String endpoint = engineUtil.getAirflowActiveDagRunsCountUrl();
    ClientResponse clientResponse = callAirflowApi(airflowConfig, isAirflow3, endpoint, HttpMethod.GET,
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
