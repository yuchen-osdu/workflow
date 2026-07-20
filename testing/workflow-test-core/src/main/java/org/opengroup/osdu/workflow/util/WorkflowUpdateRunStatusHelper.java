/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.util;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_DETAILS_WORKFLOW_RUN_URL;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowPayload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.sun.jersey.api.client.ClientResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.HttpMethod;

public interface WorkflowUpdateRunStatusHelper {

  Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  List<String> activeWorkflowStatusTypes = Arrays.asList("submitted", "running");

  static ClientResponse sendWorkflowRunFinishedUpdateRequest(HTTPClient client, String workflowName,
      String runId, Map<String, String> headers) throws Exception {
    return client.send(
        HttpMethod.PUT,
        String.format(GET_DETAILS_WORKFLOW_RUN_URL, workflowName, runId),
        buildUpdateWorkflowPayload(),
        headers,
        client.getAccessToken()
    );
  }

  static List<String> getWorkflowRuns(HTTPClient client, String workflowName,
      Map<String, String> headers) throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CREATE_WORKFLOW_RUN_URL, workflowName),
        null,
        headers,
        client.getAccessToken()
    );

    String respBody = response.getEntity(String.class);

    JsonElement responseDataArr = gson.fromJson(respBody, JsonElement.class);
    ArrayList<String> runIds = new ArrayList<>();

    if (responseDataArr instanceof JsonArray) {
      for (JsonElement responseData : (JsonArray) responseDataArr) {
        String workflowStatus = responseData.getAsJsonObject().get("status").getAsString();
        if (activeWorkflowStatusTypes.contains(workflowStatus)) {
          runIds.add(responseData.getAsJsonObject().get("runId").getAsString());
        }
      }
    }

    return runIds;
  }
}
