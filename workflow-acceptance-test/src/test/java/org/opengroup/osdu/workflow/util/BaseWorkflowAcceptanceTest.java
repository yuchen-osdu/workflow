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

package org.opengroup.osdu.workflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.workflow.consts.TestConstants.FINISHED_WORKFLOW_RUN_STATUSES;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowRunValidPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayloadExternalAirflow;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildUpdateWorkflowPayload;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.core.test.base.BaseAcceptanceTests;
import org.opengroup.osdu.core.test.client.EntitlementsClient;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.RetryConfiguration;
import org.opengroup.osdu.core.test.client.WorkflowClient;
import org.opengroup.osdu.core.test.client.model.entitlements.GroupsResponse;
import org.opengroup.osdu.core.test.client.model.workflow.WorkflowInfo;
import org.opengroup.osdu.core.test.client.model.workflow.WorkflowRunInfo;
import org.opengroup.osdu.core.test.config.TestInitializer;
import org.opengroup.osdu.core.test.service.ServiceType;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.workflow.util.v3.CreatedWorkflowRun;

/**
 * Base class for workflow acceptance tests using os-core-test:0.2.4.
 *
 * <p>Provides a shared {@link WorkflowClient} for all workflow API calls and lifecycle
 * helpers for creating, tracking, and cleaning up workflows and workflow runs.
 */
@Slf4j
public abstract class BaseWorkflowAcceptanceTest extends BaseAcceptanceTests {
    // ─── Test constants ───────────────────────────────────────────────────────
    protected static final String INVALID_WORKFLOW_NAME = "this-is-an-invalid-workflow-name";
    protected static final String INVALID_WORKFLOW_RUN_ID = "invalid-workflow-run-id";
    protected static final String INVALID_PARTITION = "invalid-partition";

    // ─── Shared typed client (static so cleanupWorkflowPreRun can use it) ─────
    protected static WorkflowClient workflowClient;
    protected static WorkflowClient unauthenticatedWorkflowClient;
    private static EntitlementsClient entitlementsClient;

    // ─── Instance state ───────────────────────────────────────────────────────
    protected String lastCreatedWorkflowName;
    protected List<CreatedWorkflowRun> createdWorkflowRuns = new ArrayList<>();

    private static final List<String> ACTIVE_WORKFLOW_STATUS_TYPES = Arrays.asList("submitted", "running");

    // ─── Constructor ──────────────────────────────────────────────────────────

    protected BaseWorkflowAcceptanceTest() {
        super(List.of(UserType.PRIVILEGED_USER, UserType.NO_ACCESS_USER), List.of(ServiceType.WORKFLOW_V1, ServiceType.ENTITLEMENTS_V2));
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Initialises the shared {@link TestInitializer} singleton and the static
     * {@link #workflowClient} before any subclass {@code @BeforeAll} methods run.
     *
     * <p>This method must remain static so it is available for the static
     * {@link #cleanupWorkflowPreRun(String)} helper called from subclass
     * {@code @BeforeAll} methods.
     */
    @BeforeAll
    static void setUpBase() {
        TestInitializer ti = TestInitializer.getSharedTestInitializer(
                List.of(UserType.PRIVILEGED_USER, UserType.NO_ACCESS_USER),
                List.of(ServiceType.WORKFLOW_V1, ServiceType.ENTITLEMENTS_V2),
                RetryConfiguration.none());
        workflowClient = new WorkflowClient(ti.getStringHttpClient(), UserType.PRIVILEGED_USER);
        unauthenticatedWorkflowClient = new WorkflowClient(ti.getStringHttpClient(), UserType.PRIVILEGED_USER, false);
        entitlementsClient = new EntitlementsClient(ti.getStringHttpClient(), UserType.PRIVILEGED_USER);
    }

    @BeforeEach
    public void setup() {
        lastCreatedWorkflowName = null;
        createdWorkflowRuns = new ArrayList<>();
    }

    @AfterEach
    public void teardown() {
        workflowClient.teardown();
    }

    // ─── Workflow creation helpers ────────────────────────────────────────────

    protected String getLastCreatedWorkflowName() {
        return lastCreatedWorkflowName;
    }

    protected void createAndTrackWorkflow() {
        WorkflowInfo info = workflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayload()).body();
        lastCreatedWorkflowName = info.workflowName();
    }

    protected void createAndTrackWorkflowExternalAirflow() {
        WorkflowInfo info = workflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayloadExternalAirflow()).body();
        lastCreatedWorkflowName = info.workflowName();
    }

    protected WorkflowRunInfo createAndTrackWorkflowRun() {
        String workflowName = getLastCreatedWorkflowName();
        WorkflowRunInfo info = workflowClient.createWorkflowRun(UserType.PRIVILEGED_USER, workflowName, buildCreateWorkflowRunValidPayload()).body();
        createdWorkflowRuns.add(new CreatedWorkflowRun(workflowName, info.runId()));
        return info;
    }

    protected WorkflowRunInfo createAndTrackWorkflowRunExternalAirflow() {
        return createAndTrackWorkflowRun();
    }

    protected WorkflowInfo createSystemWorkflow() {
        return workflowClient.createSystemWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayload()).body();
    }

    protected void createAndTrackSystemWorkflow() {
        WorkflowInfo info = createSystemWorkflow();
        lastCreatedWorkflowName = info.workflowName();
    }

    // ─── Workflow run helpers ─────────────────────────────────────────────────

    protected String getWorkflowRunStatus(String workflowName, String workflowRunId) {
        return workflowClient.getWorkflowRun(UserType.PRIVILEGED_USER, workflowName, workflowRunId).body().status();
    }

    protected void waitForCreatedWorkflowRunsToComplete() {
        if (createdWorkflowRuns == null || createdWorkflowRuns.isEmpty()) return;
        log.info("Waiting for {} workflow runs to complete", createdWorkflowRuns.size());
        Set<CreatedWorkflowRun> unfinished = new HashSet<>(createdWorkflowRuns);
        Awaitility.await("")
                .atMost(300, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(3))
                .pollInterval(Duration.ofSeconds(15))
                .ignoreExceptions()
                .until(() -> {
                    Iterator<CreatedWorkflowRun> it = unfinished.iterator();
                    while (it.hasNext()) {
                        CreatedWorkflowRun run = it.next();
                        try {
                            String status = getWorkflowRunStatus(run.workflowName(), run.workflowRunId());
                            if (FINISHED_WORKFLOW_RUN_STATUSES.contains(status)) it.remove();
                        } catch (Exception e) {
                            log.error("Error checking run status for {}: {}", run.workflowRunId(), e.getMessage());
                        }
                    }
                    return unfinished.isEmpty();
                });
    }

    protected void sendWorkflowRunFinishedUpdateRequestToCreatedWorkflowRuns() {
        if (createdWorkflowRuns == null || createdWorkflowRuns.isEmpty()) return;
        log.info("Sending finished update for {} workflow runs", createdWorkflowRuns.size());
        for (CreatedWorkflowRun run : createdWorkflowRuns) {
            try {
                String status = getWorkflowRunStatus(run.workflowName(), run.workflowRunId());
                if (!ACTIVE_WORKFLOW_STATUS_TYPES.contains(status)) {
                    log.info("Skipping finished update for run {} with status {}",
                            run.workflowRunId(), status);
                    continue;
                }
                workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER,
                        run.workflowName(), run.workflowRunId(), buildUpdateWorkflowPayload());
            } catch (Exception e) {
                log.error("Error finishing run {} for workflow {}: {}",
                        run.workflowRunId(), run.workflowName(), e.getMessage());
            }
        }
    }

    // ─── Entitlements helper ──────────────────────────────────────────────────

    protected Optional<String> getUserFromEntitlements(Map<String, String> overrideHeaders) {
        HttpResponse<GroupsResponse> response = entitlementsClient.listGroups(Map.of(), overrideHeaders);
        assertEquals(HttpStatus.SC_OK, response.statusCode(), String.valueOf(response.body()));
        GroupsResponse body = response.body();
        return body != null && body.memberEmail() != null
                ? Optional.of(body.memberEmail())
                : Optional.empty();
    }

    // ─── Static cleanup (for @BeforeAll in subclasses) ───────────────────────

    /**
     * Pre-run cleanup: finishes any active runs then deletes the named workflow.
     * Safe to call when the workflow does not exist yet.
     *
     * <p>Must only be called after {@link #setUpBase()} has initialised
     * {@link #workflowClient}.
     *
     * @param workflowName workflow name to clean up
     */
    protected static void cleanupWorkflowPreRun(String workflowName) {
        try {
            log.info("Pre-run cleanup: workflow '{}'", workflowName);
            List<WorkflowRunInfo> runs = workflowClient.listWorkflowRuns(
                    UserType.PRIVILEGED_USER, workflowName).body();
            for (WorkflowRunInfo run : runs) {
                if (ACTIVE_WORKFLOW_STATUS_TYPES.contains(run.status())) {
                    workflowClient.updateWorkflowRun(UserType.PRIVILEGED_USER,
                            workflowName, run.runId(), buildUpdateWorkflowPayload());
                }
            }
            workflowClient.deleteWorkflow(UserType.PRIVILEGED_USER, workflowName);
        } catch (Exception e) {
            log.warn("Pre-run cleanup of workflow '{}' failed (may not exist yet): {}",
                    workflowName, e.getMessage());
        }
    }
}
