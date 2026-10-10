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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithNoWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithOnlyWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayload;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.test.client.ClientException;
import org.opengroup.osdu.workflow.util.BaseWorkflowAcceptanceTest;

@Slf4j
public final class PostCreateSystemWorkflowV3IntegrationTests extends BaseWorkflowAcceptanceTest {

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
        workflowClient.cleanup("system:" + CREATE_WORKFLOW_WORKFLOW_NAME);
    }

    @Test
    public void should_returnWorkflowExists_when_givenDuplicateCreateWorkflowRequest() {
        createAndTrackSystemWorkflow();

        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createSystemWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayload()));
        assertEquals(HttpStatus.SC_CONFLICT, ex.getStatusCode());
    }

    @Test
    public void should_returnBadRequest_when_givenInvalidRequestWithNoWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createSystemWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowPayloadWithNoWorkflowName()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void should_returnBadRequest_when_givenInvalidRequestWithOnlyWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createSystemWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowPayloadWithOnlyWorkflowName()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void should_returnBadRequest_when_givenInvalidRequestWithIncorrectWorkflowName() {
        ClientException ex = assertThrows(ClientException.class,
                () -> workflowClient.createSystemWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowPayloadWithIncorrectWorkflowName()));
        assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void should_returnForbidden_when_notGivenAccessToken() {
        ClientException ex = assertThrows(ClientException.class,
                () -> unauthenticatedWorkflowClient.createSystemWorkflow(UserType.PRIVILEGED_USER, buildCreateWorkflowValidPayload()));
        assertTrue(ex.getStatusCode() == HttpStatus.SC_FORBIDDEN || ex.getStatusCode() == HttpStatus.SC_UNAUTHORIZED,
                "Expected 401/403, got: " + ex.getStatusCode());
    }
}
