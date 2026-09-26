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

package org.opengroup.osdu.workflow.workflow.v3;

import org.opengroup.osdu.core.test.auth.UserType;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.EXTERNAL_AIRFLOW_TESTS_ENABLED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_FINISHED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_QUEUED;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_RUNNING;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_STATUS_TYPE_SUCCESS;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayloadWithGivenRunId;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunInvalidPayloadStatus;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunInvalidRequestPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowRunValidPayloadWithGivenStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.workflow.util.BaseWorkflowAcceptanceTest;
import org.opengroup.osdu.workflow.util.TestExternalAirflow;
import org.opengroup.osdu.core.test.client.model.workflow.WorkflowRunInfo;
import org.opengroup.osdu.workflow.util.v3.CreatedWorkflowRun;


public final class WorkflowRunV3IntegrationTests extends BaseWorkflowAcceptanceTest {

    private static final String FINISHED = "finished";

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
    public void triggerWorkflowRun_should_returnSuccessAndCompleteExecutionWithImpersonationFlow() {
        createAndTrackWorkflow();

        Map<String, String> impersonationHeader = Map.of("on-behalf-of", "impersonatetestmember@test.com");
        String workflowName = getLastCreatedWorkflowName();
        WorkflowRunInfo workflowRunInfo = workflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, workflowName,
                buildCreateWorkflowRunValidPayload(), impersonationHeader).body();
        createdWorkflowRuns.add(new CreatedWorkflowRun(workflowName, workflowRunInfo.runId()));

        assertNotNull(workflowRunInfo.submittedBy());

        Optional<String> userFromEntitlements = getUserFromEntitlements(impersonationHeader);
        // Skip the email assertion when the entitlements response does not include
        // memberEmail (e.g. in environments where the field has a different name).
        assumeTrue(userFromEntitlements.isPresent(),
                "Skipping submittedBy assertion: entitlements /groups did not return memberEmail");
        assertEquals(userFromEntitlements.get(), workflowRunInfo.submittedBy());
    }

    @Test
    public void shouldReturn200WhenTriggerNewWorkflow() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();

        assertNotNull(workflowRunInfo.runId());
    }

    @Test
    public void shouldReturn200WhenGetAllRunInstances() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();

        HttpResponse<List<WorkflowRunInfo>> response = workflowClient.listWorkflowRuns(
                UserType.PRIVILEGED_USER, getLastCreatedWorkflowName());

        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertFalse(response.body().isEmpty());
    }

    @Test
    public void shouldReturn400WhenGetDetailsForSpecificWorkflowRunInstance() {
        String workflowName = UUID.randomUUID().toString().replace("-", "");
        String runId = UUID.randomUUID().toString().replace("-", "");

        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, workflowName, runId));

        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void shouldReturn200WhenUpdateWorkflowRunInstance() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();

        HttpResponse<WorkflowRunInfo> response = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER,
                getLastCreatedWorkflowName(), workflowRunInfo.runId(),
                buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED));

        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertEquals(workflowRunInfo.runId(), response.body().runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, response.body().status());
    }

    /** GET WORKFLOW RUN BY ID **/

    @Test
    public void getWorkflowRunById_should_returnSuccess_when_givenValidRequest() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        HttpResponse<WorkflowRunInfo> response = workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER,
                getLastCreatedWorkflowName(), workflowRunInfo.runId());
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
    }

    @TestExternalAirflow
    void getWorkflowRunById_should_returnSuccess_when_givenValidRequestOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();
        HttpResponse<WorkflowRunInfo> response = workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER,
                getLastCreatedWorkflowName(), workflowRunInfo.runId());
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
    }

    @Test
    public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME, INVALID_WORKFLOW_RUN_ID));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowRunId() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @TestExternalAirflow
    public void getWorkflowRunById_should_returnNotFound_when_givenInvalidWorkflowRunIdOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), INVALID_WORKFLOW_RUN_ID));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getWorkflowRunById_should_returnUnauthorized_when_notGivenAccessToken() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId()));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void getWorkflowRunById_should_returnUnauthorized_when_givenNoDataAccessToken() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflowRun(UserType.NO_ACCESS_USER, getLastCreatedWorkflowName(), workflowRunInfo.runId()));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getWorkflowRunById_should_returnUnauthorized_when_givenInvalidPartition() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    /** POST TRIGGER WORKFLOW RUN **/

    @Test
    public void triggerWorkflowRun_should_returnSuccessAndCompleteExecution_when_givenValidTriggerRequest() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
    }

    @TestExternalAirflow
    public void triggerWorkflowRun_should_returnSuccessAndCompleteExecution_when_givenValidTriggerRequestOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        createAndTrackWorkflowRunExternalAirflow();
    }

    @Test
    public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunId() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();

        var duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(workflowRunInfo.runId());
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), duplicateRunIdPayload));
        assertEquals(HttpStatus.SC_CONFLICT, ex.getStatusCode());
    }

    @TestExternalAirflow
    public void triggerWorkflowRun_should_returnConflict_when_givenDuplicateRunIdOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

        var duplicateRunIdPayload = buildCreateWorkflowRunValidPayloadWithGivenRunId(workflowRunInfo.runId());
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), duplicateRunIdPayload));
        assertEquals(HttpStatus.SC_CONFLICT, ex.getStatusCode());
    }

    @Test
    public void triggerWorkflowRun_should_return_WorkflowNotFound_when_givenInvalidWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME, buildCreateWorkflowRunValidPayload()));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void triggerWorkflowRun_should_returnUnauthorized_when_notGivenAccessToken() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        buildCreateWorkflowRunValidPayload()));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void triggerWorkflowRun_should_returnUnauthorized_when_givenNoDataAccessToken() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflowRun(UserType.NO_ACCESS_USER, getLastCreatedWorkflowName(), buildCreateWorkflowRunValidPayload()));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void triggerWorkflowRun_should_returnUnauthorized_when_givenInvalidPartition() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        buildCreateWorkflowRunValidPayload(), Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    /** GET ALL RUN INSTANCES **/

    @Test
    public void getAllRunInstances_should_returnSuccess_when_givenValidRequest() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        HttpResponse<List<WorkflowRunInfo>> response = workflowClient.listWorkflowRuns(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName());
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertFalse(response.body().isEmpty());
    }

    @TestExternalAirflow
    public void getAllRunInstances_should_returnSuccess_when_givenValidRequestOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        createAndTrackWorkflowRunExternalAirflow();
        HttpResponse<List<WorkflowRunInfo>> response = workflowClient.listWorkflowRuns(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName());
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertFalse(response.body().isEmpty());
    }

    @Test
    public void getAllRunInstances_should_returnSuccess_when_givenLimitParam() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        // limit=1: response must be 200 and contain at most 1 entry
        HttpResponse<List<WorkflowRunInfo>> response = workflowClient.listWorkflowRunsWithQuery(
                UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), "?limit=1");
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertTrue(response.body().size() <= 1, "Expected at most 1 result with limit=1, got: " + response.body().size());
    }

    @Test
    public void getAllRunInstances_should_returnSuccess_when_givenStartDateParam() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        // startDate=0 (epoch): all runs should be returned since epoch is before any real run
        HttpResponse<List<WorkflowRunInfo>> response = workflowClient.listWorkflowRunsWithQuery(
                UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), "?startDate=0");
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertFalse(response.body().isEmpty(), "Expected at least 1 run with startDate=0");
    }

    @Test
    public void getAllRunInstances_should_returnNotFound_when_givenInvalidWorkflowName() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.listWorkflowRuns(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getAllRunInstances_should_returnUnauthorized_when_givenNoDataAccessToken() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.listWorkflowRuns(UserType.NO_ACCESS_USER, INVALID_WORKFLOW_NAME));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getAllRunInstances_should_returnForbidden_when_notGivenAccessToken() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.listWorkflowRuns(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void getAllRunInstances_should_returnForbidden_when_givenInvalidPartition() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.listWorkflowRuns(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME, Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    /** PUT UPDATE WORKFLOW RUN STATUS **/

    @Test
    public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusRunning() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        String actualWorkflowName = getLastCreatedWorkflowName();

        HttpResponse<WorkflowRunInfo> response = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, actualWorkflowName,
                workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_RUNNING));

        WorkflowRunInfo updateInfo = response.body();
        assertEquals(HttpStatus.SC_OK, response.statusCode(), updateInfo.toString());
        assertEquals(workflowRunInfo.runId(), updateInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_RUNNING, updateInfo.status());

        String obtainedStatus = getWorkflowRunStatus(actualWorkflowName, workflowRunInfo.runId());
        List<String> expectedStatuses = List.of(WORKFLOW_STATUS_TYPE_RUNNING, WORKFLOW_STATUS_TYPE_SUCCESS, WORKFLOW_STATUS_TYPE_QUEUED);
        assertTrue(expectedStatuses.contains(obtainedStatus),
                "Expected status to be one of " + expectedStatuses + ", but got: " + obtainedStatus);
    }

    @TestExternalAirflow
    void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusRunning_onExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

        HttpResponse<WorkflowRunInfo> response = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER,
                getLastCreatedWorkflowName(), workflowRunInfo.runId(),
                buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_RUNNING));

        WorkflowRunInfo updateInfo = response.body();
        assertEquals(HttpStatus.SC_OK, response.statusCode(), updateInfo.toString());
        assertEquals(workflowRunInfo.runId(), updateInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_RUNNING, updateInfo.status());

        String obtainedStatus = getWorkflowRunStatus(getLastCreatedWorkflowName(), workflowRunInfo.runId());
        List<String> expectedStatuses = List.of(WORKFLOW_STATUS_TYPE_RUNNING, WORKFLOW_STATUS_TYPE_SUCCESS, WORKFLOW_STATUS_TYPE_QUEUED);
        assertTrue(expectedStatuses.contains(obtainedStatus),
                "Expected status to be one of " + expectedStatuses + ", but got: " + obtainedStatus);
    }

    @Test
    public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusFinished() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        String actualWorkflowName = getLastCreatedWorkflowName();

        HttpResponse<WorkflowRunInfo> response = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, actualWorkflowName,
                workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED));

        WorkflowRunInfo updateInfo = response.body();
        assertEquals(HttpStatus.SC_OK, response.statusCode(), updateInfo.toString());
        assertEquals(workflowRunInfo.runId(), updateInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, updateInfo.status());

        String obtainedStatus = getWorkflowRunStatus(actualWorkflowName, workflowRunInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, obtainedStatus);
    }

    @TestExternalAirflow
    public void updateWorkflowRunStatus_should_returnSuccess_when_givenValidRequest_StatusFinished_onExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();

        HttpResponse<WorkflowRunInfo> response = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER,
                getLastCreatedWorkflowName(), workflowRunInfo.runId(),
                buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED));

        WorkflowRunInfo updateInfo = response.body();
        assertEquals(HttpStatus.SC_OK, response.statusCode(), updateInfo.toString());
        assertEquals(workflowRunInfo.runId(), updateInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, updateInfo.status());

        String obtainedStatus = getWorkflowRunStatus(getLastCreatedWorkflowName(), workflowRunInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, obtainedStatus);
    }

    @Test
    public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidStatus() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunInvalidPayloadStatus()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @TestExternalAirflow
    public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidStatus_onExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunInvalidPayloadStatus()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidRequestPayload() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunInvalidRequestPayload()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @TestExternalAirflow
    public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenInvalidRequestPayload_onExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunInvalidRequestPayload()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnBadRequest_when_GivenCompletedWorkflowRun() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        String actualWorkflowName = getLastCreatedWorkflowName();

        HttpResponse<WorkflowRunInfo> firstResponse = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, actualWorkflowName,
                workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED));
        WorkflowRunInfo updateInfo = firstResponse.body();
        assertEquals(HttpStatus.SC_OK, firstResponse.statusCode(), updateInfo.toString());
        assertEquals(workflowRunInfo.runId(), updateInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, updateInfo.status());

        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, actualWorkflowName,
                        workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED)));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @TestExternalAirflow
    void updateWorkflowRunStatus_should_returnBadRequest_when_GivenCompletedWorkflowRun_onExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRunExternalAirflow();
        String workflowName = getLastCreatedWorkflowName();

        HttpResponse<WorkflowRunInfo> firstResponse = workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, workflowName,
                workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED));
        WorkflowRunInfo updateInfo = firstResponse.body();
        assertEquals(HttpStatus.SC_OK, firstResponse.statusCode(), updateInfo.toString());
        assertEquals(workflowRunInfo.runId(), updateInfo.runId());
        assertEquals(WORKFLOW_STATUS_TYPE_FINISHED, updateInfo.status());

        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, workflowName,
                        workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED)));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowName() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME,
                        workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(WORKFLOW_STATUS_TYPE_FINISHED)));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowRunId() {
        createAndTrackWorkflow();
        createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        INVALID_WORKFLOW_RUN_ID, buildUpdateWorkflowRunValidPayloadWithGivenStatus(FINISHED)));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @TestExternalAirflow
    void updateWorkflowRunStatus_should_returnNotFound_when_givenInvalidWorkflowRunId_onExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        createAndTrackWorkflowRunExternalAirflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        INVALID_WORKFLOW_RUN_ID, buildUpdateWorkflowRunValidPayloadWithGivenStatus(FINISHED)));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnUnauthorized_when_notGivenAccessToken() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(FINISHED)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnUnauthorized_when_givenNoDataAccessToken() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.NO_ACCESS_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(FINISHED)));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void updateWorkflowRunStatus_should_returnUnauthorized_when_givenInvalidPartition() {
        createAndTrackWorkflow();
        WorkflowRunInfo workflowRunInfo = createAndTrackWorkflowRun();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(),
                        workflowRunInfo.runId(), buildUpdateWorkflowRunValidPayloadWithGivenStatus(FINISHED),
                        Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }
}
