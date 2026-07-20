/*
 * Copyright © 2021 Amazon Web Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.aws.service.airflow.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.exception.OsduRuntimeException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WorkflowRequestBodyFactory {
  private static final String PARSE_ERROR_MSG = "Unable to parse data for dag";
  private static final String AUTH_HEADER_KEY = "authorization";
  private static final String APP_KEY_HEADER_KEY = "AppKey";

  /**
   * Serializes input to workflow queue whether from schedule rule or directly to the queue
   * @param inputParams
   * @param dagName
   * @param originalRequestHeaders
   * @param includeAuth - should be true if not from schedule, passes along auth token and app key
   * @return
   */
  public String getSerializedWorkflowRequest(Map<String, Object> inputParams, String dagName, String runId, DpsHeaders originalRequestHeaders, boolean includeAuth){
    inputParams.put("workflowRequestHeaders", getWorkflowRequestHeaders(originalRequestHeaders, includeAuth)); //include the original request headers in the body
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("conf", inputParams);

    /**
     * Must pass replace_microseconds in order for airflow to be able to run the same dag more than once per second.
     * https://github.com/apache/airflow/issues/9790
     * Fixed in Airflow 1.10.7+ (https://github.com/apache/airflow/blob/7ab62100af8a59694721e06b213a933869c6a1ed/UPDATING.md#airflow-1107)
     */
    requestBody.put("replace_microseconds", "false");
    requestBody.put("dag_name", dagName); // need the dag name so sqs sensor dag knows what dag to kick off
    requestBody.put("run_id", runId);
    return serializeData(requestBody);
  }

  /**
   * Decides whether or not to include auth headers in message to queue
   * @param originalRequestHeaders
   * @param includeAuth
   * @return
   */
  private Map<String, String> getWorkflowRequestHeaders(DpsHeaders originalRequestHeaders, boolean includeAuth){
    Map<String, String> headers = originalRequestHeaders.getHeaders();
    if(!includeAuth){
      if(headers.containsKey(AUTH_HEADER_KEY)){
        headers.remove(AUTH_HEADER_KEY);
      }
      if(headers.containsKey(APP_KEY_HEADER_KEY)){
        headers.remove(APP_KEY_HEADER_KEY);
      }
    }
    return headers;
  }

  /**
   * Helper function that serializes a dictionary into a string to be sent to airflow
   * @param data
   * @return
   */
  private String serializeData(Map<String, Object> data){
    String serializedData;
    try {
      ObjectMapper mapper = new ObjectMapper();
      serializedData = mapper.writeValueAsString(data);
      // airflow calls fail on empty bodies
      if(serializedData == null || serializedData.equals("")){
        serializedData = "{}";
      }
    } catch (JsonProcessingException e){
      throw new OsduRuntimeException(PARSE_ERROR_MSG, e);
    }
    return serializedData;
  }
}
