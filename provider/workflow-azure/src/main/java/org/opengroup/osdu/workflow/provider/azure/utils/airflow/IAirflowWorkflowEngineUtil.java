package org.opengroup.osdu.workflow.provider.azure.utils.airflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.azure.fileshare.FileShareConfig;
import org.springframework.stereotype.Component;

@Component
public interface IAirflowWorkflowEngineUtil {

  String getDagRunIdParameterName();

  String getFileNameFromWorkflow(String workflowName);

  String getAirflowDagsUrl();

  String getAirflowDagRunsUrl();

  String getAirflowDagRunsStatusUrl();

  String getAirflowActiveDagRunsCountUrl();

  String getFileShareName(FileShareConfig fileShareConfig);

  TriggerWorkflowResponse extractTriggerWorkflowResponse(String response)
      throws JsonProcessingException;

  Integer extractActiveDagRunsResponse(String response)
      throws JsonProcessingException;

  String getDagRunIdentificationParam(WorkflowEngineRequest rq);

  JSONObject addMicroSecParam(JSONObject requestBody);
}
