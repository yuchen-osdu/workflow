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
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.EXTERNAL_AIRFLOW_TESTS_ENABLED;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_CORRELATION_ID;
import static org.opengroup.osdu.workflow.consts.TestConstants.WORKFLOW_NAME_EXTERNAL_AIRFLOW;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithNoWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithOnlyWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayload;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayloadExternalAirflow;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.client.model.workflow.CreateWorkflowRequest;
import org.opengroup.osdu.core.test.client.model.workflow.WorkflowInfo;
import org.opengroup.osdu.workflow.util.BaseWorkflowAcceptanceTest;
import org.opengroup.osdu.workflow.util.TestExternalAirflow;


public final class WorkflowV3IntegrationTests extends BaseWorkflowAcceptanceTest {

    private static final String CORRELATION_ID = "test-correlation-id";

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
        super.teardown();
    }

    @Test
    public void shouldReturnSuccessWhenGivenValidRequestWorkflowCreate() {
        createAndTrackWorkflow();
    }

    @TestExternalAirflow
    void shouldReturnSuccessWhenGivenValidRequestWorkflowCreateOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
    }

    @Test
    @Disabled
    public void shouldReturnBadRequestWhenInvalidDagNameWorkflowCreate() {
        // TODO: re-enable when invalid dag name validation is active
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER,
                        org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectDag()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void shouldReturnBadRequestWhenIncorrectWorkflowNameWorkflowCreate() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowPayloadWithIncorrectWorkflowName()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void shouldContainCorrelationIdInResponseHeaders_whenGetListWorkflowForTenant_givenNoCorrelationIdInHeaders() {
        createAndTrackWorkflow();
        HttpResponse<List<WorkflowInfo>> response = workflowClient.listWorkflows(UserType.PRIVILEGED_USER);
        assertTrue(Arrays.stream(response.headers())
                .anyMatch(h -> h.getName().equalsIgnoreCase(HEADER_CORRELATION_ID)),
                "Expected correlation-id header in response");
        String corrId = Arrays.stream(response.headers())
                .filter(h -> h.getName().equalsIgnoreCase(HEADER_CORRELATION_ID))
                .map(Header::getValue)
                .findFirst().orElse(null);
        assertTrue(StringUtils.isNotBlank(corrId), "correlation-id header must not be blank");
    }

    @Test
    public void shouldContainCorrelationIdInResponseHeaders_whenGetListWorkflowForTenant_givenCorrelationIdInHeaders() {
        createAndTrackWorkflow();
        Map<String, String> corrIdHeader = Map.of(HEADER_CORRELATION_ID, CORRELATION_ID);
        HttpResponse<List<WorkflowInfo>> response = workflowClient.listWorkflows(UserType.PRIVILEGED_USER, corrIdHeader);
        String corrId = Arrays.stream(response.headers())
                .filter(h -> h.getName().equalsIgnoreCase(HEADER_CORRELATION_ID))
                .map(Header::getValue)
                .findFirst().orElse(null);
        assertEquals(CORRELATION_ID, corrId);
    }

    /** GET ALL WORKFLOWS FOR TENANT **/

    @Test
    public void getAllWorkflows_should_return200_when_getAllWorkflowsForTenant() {
        createAndTrackWorkflow();
        HttpResponse<List<WorkflowInfo>> response = workflowClient.listWorkflows(UserType.PRIVILEGED_USER, "");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertFalse(response.body().isEmpty());
    }

    @Test
    public void getAllWorkflows_should_return200_when_getAllWorkflowsForTenantWithEmptyPrefix() {
        createAndTrackWorkflow();
        // GET_ALL_WORKFLOW_PREFIX = "?prefix=" — listWorkflows(user, "") produces the same URL
        HttpResponse<List<WorkflowInfo>> response = workflowClient.listWorkflows(UserType.PRIVILEGED_USER, "");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertFalse(response.body().isEmpty());
    }

    @Test
    public void getAllWorkflows_should_returnUnauthorized_when_notGivenAccessToken() {
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.listWorkflows(UserType.PRIVILEGED_USER));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void getAllWorkflows_should_returnUnauthorized_when_givenNoDataAccessToken() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.listWorkflows(UserType.NO_ACCESS_USER));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getAllWorkflows_should_returnUnauthorized_when_givenInvalidPartition() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.listWorkflows(UserType.PRIVILEGED_USER, Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void shouldReturnBadRequestWhenGetCompleteDetailsForWorkflow() {
        createAndTrackWorkflow();
        String invalidName = "_" + getLastCreatedWorkflowName();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflow(UserType.PRIVILEGED_USER, invalidName));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    /** GET WORKFLOW BY ID **/

    @Test
    public void getWorkflowById_should_return200_when_givenValidWorkflowId() {
        createAndTrackWorkflow();
        HttpResponse<WorkflowInfo> response = workflowClient.getWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName());
        assertEquals(HttpStatus.SC_OK, response.statusCode(), response.body().toString());
        assertNotNull(response.body().workflowName());
    }

    @Test
    public void getWorkflowById_should_returnNotFound_when_givenInvalidWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflow(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void getWorkflowById_should_returnUnauthorized_when_notGivenAccessToken() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.getWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName()));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void getWorkflowById_should_returnUnauthorized_when_givenNoDataAccessToken() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflow(UserType.NO_ACCESS_USER, getLastCreatedWorkflowName()));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void getWorkflowById_should_returnForbidden_when_givenInvalidPartition() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.getWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    /** POST CREATE WORKFLOW **/

    @Test
    public void createWorkflow_should_returnWorkflowExists_when_givenDuplicateCreateWorkflowRequest() {
        CreateWorkflowRequest payload = buildCreateWorkflowValidPayload();

        HttpResponse<WorkflowInfo> firstResponse = workflowClient.createWorkflow(UserType.PRIVILEGED_USER, payload);
        assertEquals(HttpStatus.SC_OK, firstResponse.statusCode(), firstResponse.body().toString());
        lastCreatedWorkflowName = firstResponse.body().workflowName();

        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER, payload));
        assertEquals(HttpStatus.SC_CONFLICT, ex.getStatusCode());
    }

    @TestExternalAirflow
    void createWorkflow_should_returnWorkflowExists_when_givenDuplicateCreateWorkflowRequestOnExternalAirflow() {
        CreateWorkflowRequest payload = buildCreateWorkflowValidPayloadExternalAirflow();

        HttpResponse<WorkflowInfo> firstResponse = workflowClient.createWorkflow(UserType.PRIVILEGED_USER, payload);
        assertEquals(HttpStatus.SC_OK, firstResponse.statusCode(), firstResponse.body().toString());
        lastCreatedWorkflowName = firstResponse.body().workflowName();

        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER, payload));
        assertEquals(HttpStatus.SC_CONFLICT, ex.getStatusCode());
    }

    @Test
    public void createWorkflow_should_returnBadRequest_when_givenInvalidRequestWithNoWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowPayloadWithNoWorkflowName()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createWorkflow_should_returnBadRequest_when_givenInvalidRequestWithOnlyWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowPayloadWithOnlyWorkflowName()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void createWorkflow_should_returnForbidden_when_notGivenAccessToken() {
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayload()));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void createWorkflow_should_returnUnauthorized_when_givenNoDataAccessToken() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.NO_ACCESS_USER, buildCreateWorkflowValidPayload()));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void createWorkflow_should_returnForbidden_when_givenInvalidPartition() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayload(), Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    /** DELETE WORKFLOW BY ID **/

    @Test
    public void deleteWorkflow_should_delete_when_givenValidWorkflowId() {
        createAndTrackWorkflow();
        HttpResponse<Void> response = workflowClient.deleteWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName());
        assertEquals(HttpStatus.SC_NO_CONTENT, response.statusCode());
    }

    @TestExternalAirflow
    void deleteWorkflow_should_delete_when_givenValidWorkflowIdOnExternalAirflow() {
        createAndTrackWorkflowExternalAirflow();
        HttpResponse<Void> response = workflowClient.deleteWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName());
        assertEquals(HttpStatus.SC_NO_CONTENT, response.statusCode());
    }

    @Test
    public void deleteWorkflow_shouldReturnNotFound_when_givenInvalidWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.deleteWorkflow(UserType.PRIVILEGED_USER, INVALID_WORKFLOW_NAME));
        assertEquals(HttpStatus.SC_NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void deleteWorkflow_should_returnForbidden_when_notGivenAccessToken() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.deleteWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName()));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }

    @Test
    public void deleteWorkflow_should_returnUnauthorized_when_givenNoDataAccessToken() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.deleteWorkflow(UserType.NO_ACCESS_USER, getLastCreatedWorkflowName()));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    public void deleteWorkflow_should_returnForbidden_when_givenInvalidPartition() {
        createAndTrackWorkflow();
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.deleteWorkflow(UserType.PRIVILEGED_USER, getLastCreatedWorkflowName(), Map.of("data-partition-id", INVALID_PARTITION)));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }
}
