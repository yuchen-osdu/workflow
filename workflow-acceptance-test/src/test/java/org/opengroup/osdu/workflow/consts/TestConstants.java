/*
 *  Copyright 2020-2026 Google LLC
 *  Copyright 2020-2026 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.consts;

import java.util.Arrays;
import java.util.List;
import org.opengroup.osdu.core.test.config.EnvLoader;

public class TestConstants {
	// Workflow status values
	public static final String WORKFLOW_STATUS_TYPE_FINISHED = "finished";
	public static final String WORKFLOW_STATUS_TYPE_SUCCESS = "success";
	public static final String WORKFLOW_STATUS_TYPE_FAILED = "failed";
	public static final String WORKFLOW_STATUS_TYPE_QUEUED = "queued";
	public static final String WORKFLOW_STATUS_TYPE_RUNNING = "running";
	public static final List<String> FINISHED_WORKFLOW_RUN_STATUSES = Arrays.asList(
			WORKFLOW_STATUS_TYPE_FINISHED, WORKFLOW_STATUS_TYPE_FAILED, WORKFLOW_STATUS_TYPE_SUCCESS);
	public static final String CREATE_WORKFLOW_WORKFLOW_NAME = EnvLoader.get("TEST_DAG_NAME", "airflow_monitoring");
	public static final String DATA_PARTITION_ID_TENANT = EnvLoader.get("DATA_PARTITION_ID", "");
	public static final String HEADER_CORRELATION_ID = "correlation-id";
	public static final boolean EXTERNAL_AIRFLOW_TESTS_ENABLED =
			Boolean.parseBoolean(EnvLoader.get("EXTERNAL_AIRFLOW_TESTS_ENABLED", "false"));
	public static final String WORKFLOW_NAME_EXTERNAL_AIRFLOW =
			EnvLoader.get("WORKFLOW_NAME_EXTERNAL_AIRFLOW", "external-airflow-accept-test");
	public static final String TEST_DAG_NAME_EXTERNAL_AIRFLOW =
			EnvLoader.get("TEST_DAG_NAME_EXTERNAL_AIRFLOW", CREATE_WORKFLOW_WORKFLOW_NAME);
	public static final String EXTERNAL_AIRFLOW_SECRET =
			EnvLoader.get("EXTERNAL_AIRFLOW_SECRET", "airflow-workflow-tests");

	// Airflow 3 engine assertions
	public static final boolean AIRFLOW3_TESTS_ENABLED =
			Boolean.parseBoolean(EnvLoader.get("AIRFLOW3_TESTS_ENABLED", "false"));
	public static final String AIRFLOW3_EXPECTED_VERSION_PREFIX =
			EnvLoader.get("AIRFLOW3_EXPECTED_VERSION_PREFIX", "3");
}
