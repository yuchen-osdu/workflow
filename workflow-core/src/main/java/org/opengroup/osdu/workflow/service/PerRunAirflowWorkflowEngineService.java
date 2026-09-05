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

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.model.TriggerWorkflowResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;

/**
 * Dispatches each call to the engine that owns the run, based on the request's persisted
 * {@link WorkflowEngineRequest#getEngineVersion()}. Enables a single deployment to talk to both
 * Airflow 2 (default host) and Airflow 3 (side-by-side host) so in-flight Airflow 2 runs keep
 * resolving on Airflow 2 while new runs go to Airflow 3.
 *
 * <p>Selection is driven purely by the per-run engine version (configured default at trigger,
 * persisted value on status/log). There is no request-header override. A {@code null}/blank version
 * routes to Airflow 2, preserving backward compatibility for runs created before engine-ownership
 * metadata existed.
 */
@Slf4j
public class PerRunAirflowWorkflowEngineService implements IWorkflowEngineService {

  private final Map<String, IWorkflowEngineService> enginesByVersion;
  private final String defaultVersion;

  public PerRunAirflowWorkflowEngineService(
      Map<String, IWorkflowEngineService> enginesByVersion, String defaultVersion) {
    if (enginesByVersion == null || enginesByVersion.isEmpty()) {
      throw new IllegalArgumentException("At least one workflow engine must be registered");
    }
    String normalizedDefault = AirflowEngineVersions.normalize(defaultVersion);
    if (!enginesByVersion.containsKey(normalizedDefault)) {
      throw new IllegalArgumentException(
          "Default engine version '" + defaultVersion + "' has no registered engine");
    }
    this.enginesByVersion = enginesByVersion;
    this.defaultVersion = normalizedDefault;
  }

  private IWorkflowEngineService engineFor(WorkflowEngineRequest rq) {
    String requested = rq == null ? null : rq.getEngineVersion();
    // A null/blank engineVersion is a legacy run created before engine-ownership
    // metadata existed; route it to Airflow 2 for backward compatibility (never
    // to the configured default, which may be Airflow 3).
    String key =
        (requested == null || requested.isBlank())
            ? AirflowEngineVersions.V2
            : AirflowEngineVersions.normalize(requested);
    IWorkflowEngineService engine = enginesByVersion.get(key);
    if (engine == null) {
      // An unknown, non-null engine version — e.g. a legacy "v1" run stamped before Airflow 3 was
      // enabled. Fall back to Airflow 2 (never the configured default, which is Airflow 3 here) so
      // status/delete hit the engine that actually owns the run, consistent with
      // InternalAirflowExtensions.forEngineVersion and the Azure AirflowEngineUtilSelector. Routing
      // such a run to Airflow 3 would 404 and silently orphan the real DAG on the Airflow 2 host.
      IWorkflowEngineService fallback =
          enginesByVersion.getOrDefault(
              AirflowEngineVersions.V2, enginesByVersion.get(defaultVersion));
      log.warn(
          "No engine registered for version '{}'; falling back to Airflow 2. Registered: {}",
          requested,
          enginesByVersion.keySet());
      return fallback;
    }
    return engine;
  }

  @Override
  public void createWorkflow(
      WorkflowEngineRequest rq, Map<String, Object> registrationInstruction) {
    engineFor(rq).createWorkflow(rq, registrationInstruction);
  }

  @Override
  public void deleteWorkflow(WorkflowEngineRequest rq) {
    engineFor(rq).deleteWorkflow(rq);
  }

  @Override
  public void saveCustomOperator(String customOperatorDefinition, String fileName) {
    enginesByVersion.get(defaultVersion).saveCustomOperator(customOperatorDefinition, fileName);
  }

  @Override
  public TriggerWorkflowResponse triggerWorkflow(
      WorkflowEngineRequest rq, Map<String, Object> context) {
    return engineFor(rq).triggerWorkflow(rq, context);
  }

  @Override
  public WorkflowStatusType getWorkflowRunStatus(WorkflowEngineRequest rq) {
    return engineFor(rq).getWorkflowRunStatus(rq);
  }

  @Override
  public Optional<String> getVersion() {
    return enginesByVersion.get(defaultVersion).getVersion();
  }
}
