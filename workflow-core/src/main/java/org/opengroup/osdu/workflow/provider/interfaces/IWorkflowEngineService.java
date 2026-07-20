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

package org.opengroup.osdu.workflow.provider.interfaces;

import java.util.Map;
import java.util.Optional;

import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;

public interface IWorkflowEngineService {
  /**
   * Saves the workflow definition into workflow engine accessible location.
   * @param rq request parameters required to make a call to Workflow Engine.
   * @param registrationInstruction Definition of workflow.
   */
  void createWorkflow(final WorkflowEngineRequest rq, final Map<String, Object> registrationInstruction);

  /**
   * Deletes the workflow definition in workflow engine accessible location.
   * @param rq request parameters required to make a call to Workflow Engine.
   */
  void deleteWorkflow(final WorkflowEngineRequest rq);

  /**
   * Saves the custom operator into workflow engine accessible location.
   * @param customOperatorDefinition Custom operator definition.
   * @param fileName Name of the file with which workflow definition must be saved.
   */
  void saveCustomOperator(final String customOperatorDefinition, final String fileName);

  /**
   * Triggers given workflow by workflowName
   * @param rq request parameters required to make a call to Workflow Engine.
   * @param context context data object used by Workflow.
   * @return
   */
  TriggerWorkflowResponse triggerWorkflow(WorkflowEngineRequest rq, Map<String, Object> context);

  /**
   * Gets Status of the workflowRun
   * @param rq request parameters required to make a call to Workflow Engine.
   * @return Status of the particular workflowRun
   */
  WorkflowStatusType getWorkflowRunStatus(WorkflowEngineRequest rq);

  /**
   * Gets Airflow version
   *
   * @return Airflow version
   */
  default Optional<String> getVersion() {
    return Optional.empty();
  }
}
