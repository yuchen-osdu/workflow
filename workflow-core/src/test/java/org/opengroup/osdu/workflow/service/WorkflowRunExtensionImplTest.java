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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.workflow.service.WorkflowRunExtensionImpl.DAG_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;

@ExtendWith(MockitoExtension.class)
class WorkflowRunExtensionImplTest {

  private static final String WORKFLOW_NAME = "test_workflow";
  private static final String TEST_RUN_ID = "1efe8c03-c087-4ae8-a3f3-a31975184165";
  private static final String TEST_DAG_NAME = "test_dag";
  private static final Object TASK_DETAILS_RESULT = new Object();

  @Mock private IWorkflowEngineExtension workflowEngineExtension;

  @Mock private IWorkflowManagerService managerService;

  @Mock private IAirflowResolver airflowResolver;

  @InjectMocks private WorkflowRunExtensionImpl workflowRunExtension;

  @Mock private WorkflowMetadata workflowMetadata;

  @Test
  void should_ReturnTaskDetailsByDagName_when_RegistrationInstructionsContainDagName() {
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, TEST_DAG_NAME);
    mocksForInstructions(instructions);
    String expectedDagNameToGetLatestTaskDetails = TEST_DAG_NAME;
    when(workflowEngineExtension.getLatestTaskDetails(
            expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension)
        .getLatestTaskDetails(expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID);
  }

  @Test
  void should_ReturnTaskDetailsByWorkflowName_when_RegistrationInstructionsIsNull() {
    mocksForInstructions(null);
    when(workflowMetadata.getWorkflowName()).thenReturn(WORKFLOW_NAME);
    String expectedDagNameToGetLatestTaskDetails = WORKFLOW_NAME;
    when(workflowEngineExtension.getLatestTaskDetails(
            expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension)
        .getLatestTaskDetails(expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID);
  }

  @Test
  void should_ReturnTaskDetailsByWorkflowName_when_RegistrationInstructionsWithoutDagName() {
    Map<String, Object> instructions = Collections.emptyMap();
    mocksForInstructions(instructions);
    when(workflowMetadata.getWorkflowName()).thenReturn(WORKFLOW_NAME);
    String expectedDagNameToGetLatestTaskDetails = WORKFLOW_NAME;
    when(workflowEngineExtension.getLatestTaskDetails(
            expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension)
        .getLatestTaskDetails(expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID);
  }

  @Test
  void should_ReturnTaskDetailsByWorkflowName_when_RegistrationInstructionsWithNullDagName() {
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, null);
    mocksForInstructions(instructions);
    when(workflowMetadata.getWorkflowName()).thenReturn(WORKFLOW_NAME);
    String expectedDagNameToGetLatestTaskDetails = WORKFLOW_NAME;
    when(workflowEngineExtension.getLatestTaskDetails(
            expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension)
        .getLatestTaskDetails(expectedDagNameToGetLatestTaskDetails, TEST_RUN_ID);
  }

  void mocksForInstructions(Map<String, Object> instructions) {
    when(managerService.getWorkflowByName(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    when(airflowResolver.getWorkflowEngineExtension(workflowMetadata)).thenReturn(workflowEngineExtension);
    when(workflowMetadata.getRegistrationInstructions()).thenReturn(instructions);
  }
}
