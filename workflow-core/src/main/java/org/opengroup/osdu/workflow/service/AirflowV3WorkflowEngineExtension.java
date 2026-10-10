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

import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;

/**
 * Airflow 3 run-details extension targeting the public {@code api/v2/...} task-instance and XCom
 * endpoints. Only the Airflow 3 endpoint templates live here; all traversal logic is shared in
 * {@link BaseAirflowWorkflowEngineExtension}. Airflow 2 and Airflow 3 extensions are independent
 * siblings of the shared base.
 */
public class AirflowV3WorkflowEngineExtension extends BaseAirflowWorkflowEngineExtension {

  private static final String TASK_INSTANCES_V3 = "api/v2/dags/%s/dagRuns/%s/taskInstances";
  private static final String XCOM_ENTRIES_V3 = TASK_INSTANCES_V3 + "/%s/xcomEntries";
  private static final String XCOM_VALUES_V3 = XCOM_ENTRIES_V3 + "/%s";

  public AirflowV3WorkflowEngineExtension(IAirflowApiClient airflowApiClient) {
    super(airflowApiClient);
  }

  @Override
  protected String taskInstancesEndpointTemplate() {
    return TASK_INSTANCES_V3;
  }

  @Override
  protected String xcomEntriesEndpointTemplate() {
    return XCOM_ENTRIES_V3;
  }

  @Override
  protected String xcomValuesEndpointTemplate() {
    return XCOM_VALUES_V3;
  }
}
