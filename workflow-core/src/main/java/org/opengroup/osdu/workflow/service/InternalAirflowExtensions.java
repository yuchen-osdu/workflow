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
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;

/**
 * Holds the internal (non-external) run-details extensions keyed by engine version so {@code
 * /latestInfo} can be routed to the engine that owns each run. A {@code null}/blank or unknown
 * version resolves to Airflow 2, keeping legacy runs on Airflow 2 during an AF2 -&gt; AF3 migration.
 */
public class InternalAirflowExtensions {

  private final Map<String, IWorkflowEngineExtension> byEngineVersion;
  private final IWorkflowEngineExtension defaultExtension;

  public InternalAirflowExtensions(
      Map<String, IWorkflowEngineExtension> byEngineVersion,
      IWorkflowEngineExtension defaultExtension) {
    this.byEngineVersion = byEngineVersion;
    this.defaultExtension = defaultExtension;
  }

  public IWorkflowEngineExtension forEngineVersion(String engineVersion) {
    String key =
        (engineVersion == null || engineVersion.isBlank())
            ? AirflowEngineVersions.V2
            : AirflowEngineVersions.normalize(engineVersion);
    return byEngineVersion.getOrDefault(key, defaultExtension);
  }

  /**
   * {@code true} when a non-default engine (e.g. Airflow 3) is registered, so a run's persisted
   * engine version actually changes routing. When only the default (Airflow 2) engine is present —
   * the case for every Airflow 2-only provider — callers can skip the per-run DB lookup entirely and
   * resolve on the default engine, preserving pre-Airflow-3 behavior.
   */
  public boolean hasVersionedRouting() {
    return byEngineVersion.values().stream().anyMatch(ext -> ext != defaultExtension);
  }
}
