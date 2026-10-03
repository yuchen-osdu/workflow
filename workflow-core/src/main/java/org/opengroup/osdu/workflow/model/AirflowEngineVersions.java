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

package org.opengroup.osdu.workflow.model;

import java.util.Locale;

/**
 * Canonical identifiers for the supported Airflow engines.
 *
 * <p>These identifiers are persisted per {@link WorkflowRun} (engine ownership) so that status and
 * log lookups can be routed deterministically to the engine that started the run, independent of
 * the engine currently configured for new triggers. This enables an in-place AF2 -&gt; AF3
 * migration where in-flight AF2 runs continue to be tracked on AF2 while new runs go to AF3.
 */
public final class AirflowEngineVersions {

  /** Airflow 1.x — legacy {@code api/experimental/...} endpoints. */
  public static final String V1 = "v1";

  /** Airflow 2.x — stable {@code api/v1/...} endpoints. */
  public static final String V2 = "airflow2";

  /**
   * Backward-compat alias for {@link #V2}. Older configuration and persisted runs may use the short
   * {@code "v2"} identifier instead of the canonical {@code "airflow2"}.
   */
  public static final String V2_LEGACY_ALIAS = "v2";

  /** Airflow 3.x — public {@code api/v2/...} endpoints. */
  public static final String V3 = "airflow3";

  /**
   * Backward-compat / short alias for {@link #V3}. Configuration may use the short {@code "v3"}
   * identifier (the V3 engine provider also advertises it) instead of the canonical {@code
   * "airflow3"}.
   */
  public static final String V3_SHORT_ALIAS = "v3";

  private AirflowEngineVersions() {
  }

  /**
   * Normalizes a raw engine identifier to lower-case, mapping the legacy {@code "v2"} alias to
   * {@link #V2} and the short {@code "v3"} alias to {@link #V3}. Blank/null values resolve to
   * {@link #V2} for backward compatibility.
   */
  public static String normalize(String version) {
    if (version == null || version.isBlank()) {
      return V2;
    }
    String normalized = version.trim().toLowerCase(Locale.ROOT);
    if (V2_LEGACY_ALIAS.equals(normalized)) {
      return V2;
    }
    if (V3_SHORT_ALIAS.equals(normalized)) {
      return V3;
    }
    return normalized;
  }

  /** Returns {@code true} when the given identifier denotes the Airflow 3 engine. */
  public static boolean isAirflow3(String version) {
    return V3.equals(normalize(version));
  }

  /**
   * Returns {@code true} when the configured identifier explicitly selects a stable REST API engine
   * (Airflow 2 or Airflow 3), trimming and normalizing aliases ({@code v2}/{@code v3}). Blank/null
   * returns {@code false} so an unset {@code osdu.airflow.version} (which defaults to the Airflow 1
   * experimental API) does not enable Airflow 2/3-only endpoints; callers layer the legacy
   * {@code osdu.airflow.version2} flag separately.
   */
  public static boolean isStableApi(String version) {
    if (version == null || version.isBlank()) {
      return false;
    }
    String normalized = normalize(version);
    return V2.equals(normalized) || V3.equals(normalized);
  }

  /**
   * Resolves the configured engine from the two properties using a single precedence shared by the
   * runtime engine selection and the {@code /latestInfo} bean gate: an explicit
   * {@code osdu.airflow.version} always wins; otherwise the legacy {@code osdu.airflow.version2}
   * boolean selects Airflow 2 ({@code true}) or the Airflow 1 experimental API; when neither is set
   * the default is {@link #V1} (preserving historical behavior).
   */
  public static String resolveConfiguredVersion(String version, Boolean legacyVersion2) {
    if (version != null && !version.isBlank()) {
      return normalize(version);
    }
    if (Boolean.TRUE.equals(legacyVersion2)) {
      return V2;
    }
    return V1;
  }

  /**
   * {@code true} when the <em>resolved</em> engine (see {@link #resolveConfiguredVersion}) is a
   * stable REST API (Airflow 2 or 3). Unlike a raw OR of the two properties, this respects
   * precedence, so an explicit {@code osdu.airflow.version=v1} disables Airflow 2/3-only endpoints
   * even when the legacy {@code osdu.airflow.version2=true} is also (contradictorily) set.
   */
  public static boolean resolvesToStableApi(String version, Boolean legacyVersion2) {
    return isStableApi(resolveConfiguredVersion(version, legacyVersion2));
  }
}
