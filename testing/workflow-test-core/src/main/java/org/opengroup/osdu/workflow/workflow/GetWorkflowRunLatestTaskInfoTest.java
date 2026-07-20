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

package org.opengroup.osdu.workflow.workflow;

import static org.opengroup.osdu.workflow.consts.DefaultVariable.WORKFLOW_HOST;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.util.WorkflowUpdateRunStatusHelper.getWorkflowRuns;
import static org.opengroup.osdu.workflow.util.WorkflowUpdateRunStatusHelper.sendWorkflowRunFinishedUpdateRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.util.v3.TestBase;

public abstract class GetWorkflowRunLatestTaskInfoTest extends TestBase {

  public static final String GET_LATEST_DETAILS_BY_ID_API_ENDPOINT =
      getEnvironmentVariableOrDefaultKey(WORKFLOW_HOST)
          + "v1/workflow/%s/workflowRun/%s/latestInfo";
  public static final String XCOM_FIELD = "xcom";

  protected void deleteAllTestWorkflowRecords() {
    createdWorkflows.stream()
        .forEach(
            c -> {
              try {
                deleteTestWorkflows(c.get(WORKFLOW_NAME_FIELD));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Override
  protected void deleteTestWorkflows(String workflowName) throws Exception {
    List<String> runIds = getWorkflowRuns(this.client, workflowName, this.headers);
    for (String runId : runIds) {
      sendWorkflowRunFinishedUpdateRequest(this.client, workflowName, runId, this.headers);
    }
    String url = CREATE_WORKFLOW_URL + "/" + workflowName;
    sendDeleteRequest(url);
  }

  @Test
  public void testGetLatestTaskDetailsOfWorkflowRun() throws Exception {}

  @Test
  public void testGetLatestTaskDetailsOfNotExistingWorkflow() throws Exception {}

  @Test
  public void testGetLatestTaskDetailsOfNotExistingWorkflowRun() throws Exception {}

  @Test
  public void testGetLatestTaskDetailsWithoutAccess() throws Exception {}

  protected String getLatestRunDetailsUrl() throws Exception {
    String workflowResponseBody = createWorkflow();
    Map<String, String> workflowInfo =
        new ObjectMapper().readValue(workflowResponseBody, HashMap.class);

    createdWorkflows.add(workflowInfo);

    String workflowRunResponseBody = createWorkflowRun();
    Map<String, String> workflowRunInfo =
        new ObjectMapper().readValue(workflowRunResponseBody, HashMap.class);
    createdWorkflowRuns.add(workflowInfo);

    String runId = workflowRunInfo.get(WORKFLOW_RUN_ID_FIELD);

    String latestRunDetailsUrl =
        String.format(GET_LATEST_DETAILS_BY_ID_API_ENDPOINT, CREATE_WORKFLOW_WORKFLOW_NAME, runId);

    Thread.sleep(5000);
    return latestRunDetailsUrl;
  }
}
