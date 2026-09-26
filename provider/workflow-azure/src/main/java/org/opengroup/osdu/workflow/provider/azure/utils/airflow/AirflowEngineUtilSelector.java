// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.workflow.provider.azure.utils.airflow;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Selects the Airflow engine util for a request based on its persisted engine version.
 *
 * <p>Exactly one non-Airflow-3 util (Airflow 1 XOR Airflow 2) is active as the {@code baseUtil};
 * the Airflow 3 util is optional and only present when {@code osdu.airflow.version=airflow3}, in
 * which case it coexists with the Airflow 2 base for deterministic per-run routing. A request whose
 * {@code engineVersion} is {@code airflow3} routes to the Airflow 3 util; anything else (including
 * {@code null} for legacy runs) routes to the base util, preserving Airflow 2 backward
 * compatibility. There is no request-header override.
 */
@Component
@Slf4j
public class AirflowEngineUtilSelector {

  private final IAirflowWorkflowEngineUtil baseUtil;
  private final IAirflowWorkflowEngineUtil v3Util;

  @Autowired
  public AirflowEngineUtilSelector(
      List<IAirflowWorkflowEngineUtil> engineUtils,
      @Qualifier("AirflowV3WorkflowEngineUtil")
          ObjectProvider<IAirflowWorkflowEngineUtil> v3Provider) {
    this.v3Util = v3Provider.getIfAvailable();
    this.baseUtil =
        engineUtils.stream()
            .filter(util -> util != this.v3Util)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No base Airflow engine util (Airflow 1/2) is registered"));
  }

  /** Resolves the util for a request, falling back to the base util for null/blank versions. */
  public IAirflowWorkflowEngineUtil utilFor(WorkflowEngineRequest rq) {
    return utilForVersion(rq != null ? rq.getEngineVersion() : null);
  }

  public IAirflowWorkflowEngineUtil utilForVersion(String engineVersion) {
    if (AirflowEngineVersions.isAirflow3(engineVersion)) {
      if (v3Util != null) {
        return v3Util;
      }
      // A run stamped airflow3 but Airflow 3 is not (or no longer) enabled — e.g. a rollback while
      // an AF3 run is in flight. Fall back to the base (Airflow 2) util instead of hard-failing,
      // mirroring the core dispatcher's graceful degradation. Airflow then answers 404 for a run it
      // never owned, which is a far better failure mode than a 500 on every status/delete call.
      log.warn(
          "engineVersion=airflow3 requested but no AirflowV3WorkflowEngineUtil bean is registered "
              + "(Airflow 3 disabled/rolled back); falling back to the base engine util.");
      return baseUtil;
    }
    return baseUtil;
  }

  /**
   * The default engine util for operations not tied to a specific run (custom-operator upload,
   * active-run counting). Returns the configured engine's util — Airflow 3 when enabled, otherwise
   * the base (Airflow 2) util — so these operations target the same backend as new runs.
   */
  public IAirflowWorkflowEngineUtil getDefaultUtil() {
    return v3Util != null ? v3Util : baseUtil;
  }

  public boolean isV3Available() {
    return v3Util != null;
  }

  IAirflowWorkflowEngineUtil getBaseUtil() {
    return baseUtil;
  }
}
