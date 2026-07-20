package org.opengroup.osdu.workflow.provider.azure.utils.airflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AirflowV1WorkflowEngineUtilTest {
  private static final String FILE_NAME_PREFIX = ".py";
  private static final String AIRFLOW_DAGS_URL = "api/experimental/dags/%s";
  private static final String AIRFLOW_DAG_RUNS_URL = "api/experimental/dags/%s/dag_runs";
  private static final String AIRFLOW_DAG_RUNS_STATUS_URL = "api/experimental/dags/%s/dag_runs/%s";
  private final static String RUN_ID_PARAMETER_NAME = "run_id";
  private final static String AIRFLOW_MICROSECONDS_FLAG = "replace_microseconds";
  private final TriggerWorkflowResponse triggerWorkflowResponse = new TriggerWorkflowResponse();

  @Mock
  private ObjectMapper OBJECT_MAPPER;

  @Mock
  private FileShareConfig fileShareConfig;

  @Mock
  private WorkflowEngineRequest workflowEngineRequest;

  @InjectMocks
  AirflowV1WorkflowEngineUtil airflowV1WorkflowEngineUtil;

  @Test
  public void testGetDagRunIdParameterName() {
    String runIdParameterName = airflowV1WorkflowEngineUtil.getDagRunIdParameterName();
    assertEquals(RUN_ID_PARAMETER_NAME, runIdParameterName);
  }

  @Test
  public void testGetFileNameFromWorkflow() {
    String workflowName = "workflowName";
    String fileName = airflowV1WorkflowEngineUtil.getFileNameFromWorkflow(workflowName);
    assertEquals(workflowName + FILE_NAME_PREFIX, fileName);
  }

  @Test
  public void testGetAirflowDagsUrl() {
    String airflowDagsUrl = airflowV1WorkflowEngineUtil.getAirflowDagsUrl();
    assertEquals(AIRFLOW_DAGS_URL, airflowDagsUrl);
  }

  @Test
  public void testGetAirflowDagRunsUrl() {
    String airflowDagRunsUrl = airflowV1WorkflowEngineUtil.getAirflowDagRunsUrl();
    assertEquals(AIRFLOW_DAG_RUNS_URL, airflowDagRunsUrl);
  }

  @Test
  public void testGetAirflowDagRunsStatusUrl() {
    String airflowDagRunsStatusUrl = airflowV1WorkflowEngineUtil.getAirflowDagRunsStatusUrl();
    assertEquals(AIRFLOW_DAG_RUNS_STATUS_URL, airflowDagRunsStatusUrl);
  }

  @Test
  public void testGetFileShareName() {
    String fileShareName = "file-share-name";
    when(fileShareConfig.getShareName()).thenReturn(fileShareName);
    String fileShareNameObtained = airflowV1WorkflowEngineUtil.getFileShareName(fileShareConfig);
    assertEquals(fileShareName, fileShareNameObtained);
  }

  @Test
  public void testExtractTriggerWorkflowResponse() throws JsonProcessingException {
    String response = "response";
    String extractedResponse = "extracted-response";
    when(OBJECT_MAPPER.readValue(eq(response), eq(TriggerWorkflowResponse.class))).thenReturn(triggerWorkflowResponse);

    TriggerWorkflowResponse triggerWorkflowResponseObtained = airflowV1WorkflowEngineUtil.extractTriggerWorkflowResponse(response);
    assertEquals(triggerWorkflowResponse, triggerWorkflowResponseObtained);
  }

  @Test
  public void testGetDagRunIdentificationParam() {
    String workflowEngineExecutionDate = "test";
    when(workflowEngineRequest.getWorkflowEngineExecutionDate()).thenReturn(workflowEngineExecutionDate);
    String workflowEngineExecutionDateObtained = airflowV1WorkflowEngineUtil.getDagRunIdentificationParam(workflowEngineRequest);

    assertEquals(workflowEngineExecutionDate, workflowEngineExecutionDateObtained);
  }

  @Test
  public void testAddMicroSecParam() throws JSONException {
    JSONObject requestBody = mock(JSONObject.class);
    JSONObject requestBodyObtained = airflowV1WorkflowEngineUtil.addMicroSecParam(requestBody);

    verify(requestBody).put(eq(AIRFLOW_MICROSECONDS_FLAG), eq("false"));
    assertEquals(requestBody, requestBodyObtained);
  }
}
