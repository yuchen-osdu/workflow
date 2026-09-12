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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;

class PerRunAirflowWorkflowEngineServiceTest {

  private IWorkflowEngineService af2;
  private IWorkflowEngineService af3;
  private PerRunAirflowWorkflowEngineService dispatcher;

  @BeforeEach
  void setup() {
    af2 = mock(IWorkflowEngineService.class);
    af3 = mock(IWorkflowEngineService.class);
    Map<String, IWorkflowEngineService> engines = new HashMap<>();
    engines.put(AirflowEngineVersions.V2, af2);
    engines.put(AirflowEngineVersions.V2_LEGACY_ALIAS, af2);
    engines.put(AirflowEngineVersions.V3, af3);
    dispatcher = new PerRunAirflowWorkflowEngineService(engines, AirflowEngineVersions.V3);
  }

  private WorkflowEngineRequest rq(String engineVersion) {
    return WorkflowEngineRequest.builder().engineVersion(engineVersion).build();
  }

  @Test
  void trigger_routesToAirflow3_forAirflow3Run() {
    WorkflowEngineRequest rq = rq("airflow3");
    dispatcher.triggerWorkflow(rq, Map.of());
    verify(af3).triggerWorkflow(rq, Map.of());
    verifyNoInteractions(af2);
  }

  @Test
  void status_routesToAirflow2_forAirflow2Run() {
    WorkflowEngineRequest rq = rq("airflow2");
    dispatcher.getWorkflowRunStatus(rq);
    verify(af2).getWorkflowRunStatus(rq);
    verifyNoInteractions(af3);
  }

  @Test
  void status_routesToAirflow2_forLegacyV2Alias() {
    WorkflowEngineRequest rq = rq("v2");
    dispatcher.getWorkflowRunStatus(rq);
    verify(af2).getWorkflowRunStatus(rq);
  }

  @Test
  void status_routesToAirflow2_forNullVersion_backwardCompat() {
    WorkflowEngineRequest rq = rq(null);
    dispatcher.getWorkflowRunStatus(rq);
    verify(af2).getWorkflowRunStatus(rq);
  }

  @Test
  void status_routesToAirflow2_forUnknownV1Version_notDefaultAirflow3() {
    // A legacy "v1"-stamped run (default stamp when osdu.airflow.version was unset) is not in the
    // dispatcher map. It must fall back to Airflow 2 — not the configured default (Airflow 3) —
    // otherwise status/delete would hit the wrong host and silently orphan the real DAG.
    WorkflowEngineRequest rq = rq("v1");
    dispatcher.getWorkflowRunStatus(rq);
    verify(af2).getWorkflowRunStatus(rq);
    verifyNoInteractions(af3);
  }

  @Test
  void delete_routesToAirflow2_forUnknownV1Version_notDefaultAirflow3() {
    WorkflowEngineRequest rq = rq("v1");
    dispatcher.deleteWorkflow(rq);
    verify(af2).deleteWorkflow(rq);
    verifyNoInteractions(af3);
  }

  @Test
  void status_routesToAirflow2_forGenericUnknownVersion() {
    // Any unregistered non-null version (not just v1) must degrade to Airflow 2, never the AF3
    // default, so a stray/typo'd or future value can never orphan a DAG on the AF2 host.
    WorkflowEngineRequest rq = rq("foo");
    dispatcher.getWorkflowRunStatus(rq);
    verify(af2).getWorkflowRunStatus(rq);
    verifyNoInteractions(af3);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "airflow3, af3",
        "v3, af3",
        "' AIRFLOW3 ', af3",
        "airflow2, af2",
        "v2, af2",
        "v1, af2",
        "foo, af2",
        "'', af2",
        "NULL, af2"
      },
      nullValues = "NULL")
  void versionMatrix_locksRoutingContract(String engineVersion, String expected) {
    WorkflowEngineRequest rq = rq(engineVersion);
    dispatcher.getWorkflowRunStatus(rq);
    IWorkflowEngineService expectedEngine = "af3".equals(expected) ? af3 : af2;
    IWorkflowEngineService otherEngine = "af3".equals(expected) ? af2 : af3;
    verify(expectedEngine).getWorkflowRunStatus(rq);
    verifyNoInteractions(otherEngine);
  }

  @Test
  void createAndDelete_dispatchByEngineVersion() {
    WorkflowEngineRequest rq = rq("airflow2");
    dispatcher.createWorkflow(rq, Map.of());
    dispatcher.deleteWorkflow(rq);
    verify(af2).createWorkflow(rq, Map.of());
    verify(af2).deleteWorkflow(rq);
  }

  @Test
  void saveCustomOperator_usesDefaultEngine() {
    dispatcher.saveCustomOperator("def", "file.py");
    verify(af3).saveCustomOperator("def", "file.py");
  }

  @Test
  void constructor_rejectsEmptyEngines() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PerRunAirflowWorkflowEngineService(new HashMap<>(), AirflowEngineVersions.V3));
  }

  @Test
  void constructor_rejectsUnregisteredDefault() {
    Map<String, IWorkflowEngineService> engines = new HashMap<>();
    engines.put(AirflowEngineVersions.V2, af2);
    assertThrows(
        IllegalArgumentException.class,
        () -> new PerRunAirflowWorkflowEngineService(engines, AirflowEngineVersions.V3));
  }
}
