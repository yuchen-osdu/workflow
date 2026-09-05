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

package org.opengroup.osdu.workflow.config;

import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for the Airflow engine configured for NEW workflow triggers.
 *
 * <p>Selection is deployment-wide and driven purely by configuration (no request header). Runs
 * persist the resolved engine so status/log lookups route deterministically to the owning engine.
 */
@Component
public class AirflowEngineVersionProvider {

  private final String configuredVersion;

  public AirflowEngineVersionProvider(
      @Value("${osdu.airflow.version:#{null}}") String airflowVersion,
      @Value("${osdu.airflow.version2:#{null}}") Boolean legacyAirflowVersion2) {
    this.configuredVersion = resolve(airflowVersion, legacyAirflowVersion2);
  }

  /** Canonical engine version stamped onto new runs (e.g. {@code airflow2}, {@code airflow3}). */
  public String getConfiguredVersion() {
    return configuredVersion;
  }

  /**
   * Precedence delegated to {@link AirflowEngineVersions#resolveConfiguredVersion} so the runtime
   * engine selection and the {@code /latestInfo} bean gate stay in lock-step: an explicit
   * {@code osdu.airflow.version} wins; otherwise the legacy {@code osdu.airflow.version2} boolean
   * selects Airflow 2 ({@code true}) or the Airflow 1 experimental API; unset defaults to
   * {@link AirflowEngineVersions#V1}, preserving pre-existing cross-provider behavior.
   */
  private static String resolve(String airflowVersion, Boolean legacyAirflowVersion2) {
    return AirflowEngineVersions.resolveConfiguredVersion(airflowVersion, legacyAirflowVersion2);
  }
}
