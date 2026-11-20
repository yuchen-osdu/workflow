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

package org.opengroup.osdu.workflow.workflow;

import org.opengroup.osdu.core.test.auth.UserType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.EXTERNAL_AIRFLOW_TESTS_ENABLED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hc.core5.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.workflow.util.BaseWorkflowAcceptanceTest;
import org.opengroup.osdu.workflow.util.TestExternalAirflow;
import org.opengroup.osdu.core.test.client.model.workflow.WorkflowRunInfo;


public final class GetWorkflowRunLatestTaskInfoTest extends BaseWorkflowAcceptanceTest {

    public static final String XCOM_FIELD = "xcom";

    @BeforeAll
    static void beforeAll() {
        cleanupWorkflowPreRun(CREATE_WORKFLOW_WORKFLOW_NAME);
        if (EXTERNAL_AIRFLOW_TESTS_ENABLED) {
            cleanupWorkflowPreRun(WORKFLOW_NAME_EXTERNAL_AIRFLOW);
        }
    }

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
    }

    @AfterEach
    @Override
    public void teardown() {
        waitForCreatedWorkflowRunsToComplete();
        sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns();
        super.teardown();
    }

    @Test
    public void testGetLatestTaskDetailsOfWorkflowRun() {
        String workflowName = prepareWorkflowAndGetName();
        String runId = prepareWorkflowRunAndGetId();
        AtomicReference<Map<String, Object>> resultRef = new AtomicReference<>();

        // Poll until Airflow has executed at least one task and populated xcom data.
        // A fixed sleep is unreliable — Awaitility retries so the test passes as soon
        // as data is ready and fails with a clear timeout if it never arrives.
        Awaitility.await("latest task info to become available")
                .atMost(60, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(3))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    try {
                        Map<String, Object> details = workflowClient.getLatestTaskInfo(
                                UserType.PRIVILEGED_USER, workflowName, runId).body();
                        if (details.get(XCOM_FIELD) == null) return false;
                        resultRef.set(details);
                        return true;
                    } catch (ClientException e) {
                        return false;
                    }
                });

        assertNotNull(resultRef.get().get(XCOM_FIELD));
    }

    @TestExternalAirflow
    void testGetLatestTaskDetailsOfWorkflowRunOnExternalAirflow() {
        String workflowName = prepareWorkflowExternalAirflowAndGetName();
        String runId = prepareWorkflowRunAndGetId();
        AtomicReference<Map<String, Object>> resultRef = new AtomicReference<>();

        Awaitility.await("latest task info (external airflow) to become available")
                .atMost(60, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(3))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    try {
                        Map<String, Object> details = workflowClient.getLatestTaskInfo(
                                UserType.PRIVILEGED_USER, workflowName, runId).body();
                        if (details.get(XCOM_FIELD) == null) return false;
                        resultRef.set(details);
                        return true;
                    } catch (ClientException e) {
                        return false;
                    }
                });

        assertNotNull(resultRef.get().get(XCOM_FIELD));
    }

    @Test
    public void testGetLatestTaskDetailsOfNotExistingWorkflow() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getLatestTaskInfo(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void testGetLatestTaskDetailsOfNotExistingWorkflowRun() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getLatestTaskInfo(UserType.PRIVILEGED_USER, CREATE_WORKFLOW_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @TestExternalAirflow
    void testGetLatestTaskDetailsOfNotExistingWorkflowRunOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getLatestTaskInfo(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void testGetLatestTaskDetailsWithoutAccess() {
        String workflowName = prepareWorkflowAndGetName();
        String runId = prepareWorkflowRunAndGetId();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getLatestTaskInfo(UserType.NO_ACCESS_USER, workflowName, runId));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    private String prepareWorkflowAndGetName() {
        createAndTrackWorkflow();
        return CREATE_WORKFLOW_WORKFLOW_NAME;
    }

    private String prepareWorkflowExternalAirflowAndGetName() {
        createAndTrackWorkflowExternalAirflow();
        return getLastCreatedWorkflowName();
    }

    private String prepareWorkflowRunAndGetId() {
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        return workflowRunInfo.runId();
    }
}
