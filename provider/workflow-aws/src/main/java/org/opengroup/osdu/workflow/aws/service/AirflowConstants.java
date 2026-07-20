/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.workflow.aws.service;

public final class AirflowConstants {

    private AirflowConstants() {
        //Private Constructor
    }

    public static final String AIRFLOW_TRIGGER_DAG_ERROR_MESSAGE =
      "Failed to trigger workflow with id %s and name %s";
    public static final String AIRFLOW_DELETE_DAG_ERROR_MESSAGE =
        "Failed to delete workflow with name %s";
    public static final String AIRFLOW_WORKFLOW_RUN_NOT_FOUND =
        "No WorkflowRun executed for Workflow: %s on %s ";
    public static final String AIRFLOW_PAYLOAD_PARAMETER_NAME = "conf";
    public static final String RUN_ID_PARAMETER_NAME = "run_id";
    public static final String EXECUTION_DATE_PARAMETER_NAME = "execution_date";
    public static final String EXECUTION_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String FILE_NAME_PREFIX = ".py";
    public static final String AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_TRIGGER_CONFIGURATION = "_trigger_config";
    public static final String AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_WORKFLOW_ID = "trigger_dag_id";
    public static final String AIRFLOW_CONTROLLER_PAYLOAD_PARAMETER_WORKFLOW_RUN_ID = "trigger_dag_run_id";
    public static final String AIRFLOW_MICROSECONDS_FLAG = "replace_microseconds";
    public static final String KEY_DAG_CONTENT = "dagContent";
    
}
