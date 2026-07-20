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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunExtension;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowRunExtensionImpl implements IWorkflowRunExtension {
  protected static final String DAG_NAME = "dagName";

  private final IWorkflowManagerService managerService;
  private final IAirflowResolver airflowResolver;

  @Override
  public Object getLatestTaskDetails(String workflowName, String runId) {
    WorkflowMetadata workflowMetadata = managerService.getWorkflowByName(workflowName);
    Map<String, Object> instructions = workflowMetadata.getRegistrationInstructions();
    String dagName =
        instructions != null && instructions.get(DAG_NAME) != null
            ? (String) instructions.get(DAG_NAME)
            : workflowMetadata.getWorkflowName();
    return getWorkflowEngineExtension(workflowMetadata).getLatestTaskDetails(dagName, runId);
  }

  /**
   * Get the workflow engine extension.
   *
   * @param workflowMetadata WorkflowMetadata with registration instructions for extensibility
   * @return IWorkflowEngineExtension
   */
  protected IWorkflowEngineExtension getWorkflowEngineExtension(WorkflowMetadata workflowMetadata) {
    return airflowResolver.getWorkflowEngineExtension(workflowMetadata);
  }
}
