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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.workflow.service.WorkflowRunExtensionImpl.DAG_NAME;
import static org.opengroup.osdu.workflow.service.WorkflowRunExtensionImpl.EXTERNAL_AIRFLOW_SECRET;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;

@ExtendWith(MockitoExtension.class)
class WorkflowRunExtensionImplTest {

  private static final String WORKFLOW_NAME = "test_workflow";
  private static final String TEST_RUN_ID = "1efe8c03-c087-4ae8-a3f3-a31975184165";
  private static final String TEST_DAG_NAME = "test_dag";
  private static final Object TASK_DETAILS_RESULT = new Object();

  @Mock private IWorkflowEngineExtension workflowEngineExtension;
  @Mock private IWorkflowManagerService managerService;
  @Mock private IAirflowResolver airflowResolver;
  @Mock private IWorkflowRunRepository workflowRunRepository;
  @Mock private InternalAirflowExtensions internalAirflowExtensions;

  @InjectMocks private WorkflowRunExtensionImpl workflowRunExtension;

  @Mock private WorkflowMetadata workflowMetadata;
  @Mock private WorkflowRun workflowRun;

  @Test
  void should_ReturnTaskDetailsByDagName_when_RegistrationInstructionsContainDagName() {
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, TEST_DAG_NAME);
    mocksForInternalRun(instructions, AirflowEngineVersions.V2);
    when(workflowEngineExtension.getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension).getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID);
  }

  @Test
  void should_ReturnTaskDetailsByWorkflowName_when_RegistrationInstructionsIsNull() {
    mocksForInternalRun(null, AirflowEngineVersions.V2);
    when(workflowMetadata.getWorkflowName()).thenReturn(WORKFLOW_NAME);
    when(workflowEngineExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension).getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);
  }

  @Test
  void should_ReturnTaskDetailsByWorkflowName_when_RegistrationInstructionsWithoutDagName() {
    mocksForInternalRun(Collections.emptyMap(), AirflowEngineVersions.V2);
    when(workflowMetadata.getWorkflowName()).thenReturn(WORKFLOW_NAME);
    when(workflowEngineExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowEngineExtension).getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);
  }

  @Test
  void should_RouteToOwningEngine_byPersistedEngineVersion() {
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, TEST_DAG_NAME);
    mocksForInternalRun(instructions, AirflowEngineVersions.V3);
    when(workflowEngineExtension.getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(internalAirflowExtensions).forEngineVersion(AirflowEngineVersions.V3);
    verify(workflowEngineExtension).getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID);
  }

  @Test
  void should_UseExternalAirflowExtension_when_ExternalSecretPresent() {
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, TEST_DAG_NAME);
    instructions.put(EXTERNAL_AIRFLOW_SECRET, "some-secret");
    when(managerService.getWorkflowByName(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    when(workflowMetadata.getRegistrationInstructions()).thenReturn(instructions);
    when(airflowResolver.getWorkflowEngineExtension(workflowMetadata))
        .thenReturn(workflowEngineExtension);
    when(workflowEngineExtension.getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(airflowResolver).getWorkflowEngineExtension(workflowMetadata);
    verify(workflowEngineExtension).getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID);
  }

  @Test
  void should_SkipDbLookup_when_NoVersionedRouting() {
    // Airflow 2-only deployment: no non-default engine registered, so /latestInfo must NOT read the
    // run from the DB at all (avoids the 404 for runs absent from the DB) and resolves on default.
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, TEST_DAG_NAME);
    when(managerService.getWorkflowByName(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    when(workflowMetadata.getRegistrationInstructions()).thenReturn(instructions);
    when(internalAirflowExtensions.hasVersionedRouting()).thenReturn(false);
    when(internalAirflowExtensions.forEngineVersion(null)).thenReturn(workflowEngineExtension);
    when(workflowEngineExtension.getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(workflowRunRepository, never()).getWorkflowRun(WORKFLOW_NAME, TEST_RUN_ID);
    verify(internalAirflowExtensions).forEngineVersion(null);
  }

  @Test
  void should_FallBackToDefaultEngine_when_RepositoryThrowsBareAppException404() {
    // Some providers (e.g. IBM) surface a missing run as a bare AppException(404) rather than the
    // typed WorkflowRun/WorkflowNotFoundException. /latestInfo must still degrade to the default
    // engine instead of propagating the 404.
    Map<String, Object> instructions = new HashMap<>();
    instructions.put(DAG_NAME, TEST_DAG_NAME);
    when(managerService.getWorkflowByName(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    when(workflowMetadata.getRegistrationInstructions()).thenReturn(instructions);
    when(internalAirflowExtensions.hasVersionedRouting()).thenReturn(true);
    when(workflowRunRepository.getWorkflowRun(WORKFLOW_NAME, TEST_RUN_ID))
        .thenThrow(new AppException(404, "Not Found", "workflow run not found"));
    when(internalAirflowExtensions.forEngineVersion(null)).thenReturn(workflowEngineExtension);
    when(workflowEngineExtension.getLatestTaskDetails(TEST_DAG_NAME, TEST_RUN_ID))
        .thenReturn(TASK_DETAILS_RESULT);

    Object result = workflowRunExtension.getLatestTaskDetails(WORKFLOW_NAME, TEST_RUN_ID);

    assertSame(TASK_DETAILS_RESULT, result);
    verify(internalAirflowExtensions).forEngineVersion(null);
  }

  private void mocksForInternalRun(Map<String, Object> instructions, String engineVersion) {
    when(managerService.getWorkflowByName(WORKFLOW_NAME)).thenReturn(workflowMetadata);
    when(workflowMetadata.getRegistrationInstructions()).thenReturn(instructions);
    when(internalAirflowExtensions.hasVersionedRouting()).thenReturn(true);
    when(workflowRunRepository.getWorkflowRun(WORKFLOW_NAME, TEST_RUN_ID)).thenReturn(workflowRun);
    lenient().when(workflowRun.getEngineVersion()).thenReturn(engineVersion);
    when(internalAirflowExtensions.forEngineVersion(engineVersion))
        .thenReturn(workflowEngineExtension);
  }
}
