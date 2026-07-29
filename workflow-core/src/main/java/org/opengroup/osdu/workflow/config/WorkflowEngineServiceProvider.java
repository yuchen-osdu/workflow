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

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.AirflowV2WorkflowEngineExtension;
import org.opengroup.osdu.workflow.service.AirflowV3WorkflowEngineExtension;
import org.opengroup.osdu.workflow.service.InternalAirflowExtensions;
import org.opengroup.osdu.workflow.service.PerRunAirflowWorkflowEngineService;
import org.opengroup.osdu.workflow.service.factory.AirflowApiClientFactory;
import org.opengroup.osdu.workflow.service.factory.ExternalAirflowConfigFactory;
import org.opengroup.osdu.workflow.service.factory.WorkflowEngineServiceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngineServiceProvider {

  private static final String JWT_AUTH = "JwtAuth";

  private final IAirflowApiClient airflowApiClient;
  private final WorkflowEngineServiceFactory workflowEngineServiceFactory;
  private final AirflowApiClientFactory airflowApiClientFactory;
  private final ExternalAirflowConfigFactory externalAirflowConfigFactory;
  private final AirflowEngineVersionProvider airflowEngineVersionProvider;

  /** Airflow 3 backend connection (side-by-side host), used only when Airflow 3 is enabled. */
  @Value("${osdu.airflow.airflow3.url:}")
  private String airflow3Url;

  @Value("${osdu.airflow.airflow3.username:}")
  private String airflow3Username;

  @Value("${osdu.airflow.airflow3.password:}")
  private String airflow3Password;

  /**
   * Memoized Airflow 3 API client. Shared by the per-run engine and the {@code /latestInfo}
   * extension so there is a single {@code Airflow3TokenClient} token cache (one {@code /auth/token}
   * exchange per refresh window) instead of one per consumer.
   */
  private IAirflowApiClient airflow3ApiClient;

  @Bean
  IWorkflowEngineService workflowEngineService() {
    String configuredVersion = airflowEngineVersionProvider.getConfiguredVersion();

    if (AirflowEngineVersions.V3.equals(configuredVersion)) {
      requireAirflow3BackendConfigured();
      return buildPerRunEngine();
    }

    log.info("Creating single workflow engine for airflow version: {}", configuredVersion);
    return workflowEngineServiceFactory.createWorkflowEngineService(
        configuredVersion, airflowApiClient);
  }

  /**
   * Builds a per-run dispatcher wiring the Airflow 2 base engine (default host) alongside the
   * Airflow 3 engine (side-by-side host). New runs (engineVersion=airflow3) go to Airflow 3;
   * in-flight Airflow 2 runs (engineVersion=airflow2/null) keep resolving on Airflow 2.
   */
  private IWorkflowEngineService buildPerRunEngine() {
    IWorkflowEngineService airflow2Engine =
        workflowEngineServiceFactory.createWorkflowEngineService(
            AirflowEngineVersions.V2, airflowApiClient);
    IWorkflowEngineService airflow3Engine =
        workflowEngineServiceFactory.createWorkflowEngineService(
            AirflowEngineVersions.V3, airflow3ApiClient());

    Map<String, IWorkflowEngineService> engines = new LinkedHashMap<>();
    engines.put(AirflowEngineVersions.V2, airflow2Engine);
    engines.put(AirflowEngineVersions.V2_LEGACY_ALIAS, airflow2Engine);
    engines.put(AirflowEngineVersions.V3, airflow3Engine);

    log.info(
        "Airflow 3 enabled: wiring per-run engine dispatcher. default={}, engines={}",
        AirflowEngineVersions.V3,
        engines.keySet());
    return new PerRunAirflowWorkflowEngineService(engines, AirflowEngineVersions.V3);
  }

  /**
   * Lazily builds and caches the single Airflow 3 API client (and its token cache). Bean methods on
   * this configuration run once during startup on the same instance, so a plain memoized field is
   * sufficient — no synchronization needed.
   */
  private IAirflowApiClient airflow3ApiClient() {
    if (airflow3ApiClient == null) {
      airflow3ApiClient = buildAirflow3ApiClient();
    }
    return airflow3ApiClient;
  }

  private IAirflowApiClient buildAirflow3ApiClient() {
    Map<String, Object> configMap = new LinkedHashMap<>();
    configMap.put("url", airflow3Url);
    configMap.put("username", airflow3Username);
    configMap.put("password", airflow3Password);
    return airflowApiClientFactory.createAirflowApiClient(
        JWT_AUTH,
        externalAirflowConfigFactory.createExternalAirflowConfig(JWT_AUTH, configMap));
  }

  /**
   * Per-run-version internal run-details extensions (drive {@code /latestInfo}). Airflow 2 is always
   * available; Airflow 3's {@code api/v2} extension (JWT client) is added when enabled, so a run's
   * {@code /latestInfo} is resolved against the engine that owns it. Legacy/null runs use Airflow 2.
   */
  @Bean
  InternalAirflowExtensions internalAirflowExtensions() {
    IWorkflowEngineExtension airflow2Extension =
        new AirflowV2WorkflowEngineExtension(airflowApiClient);
    Map<String, IWorkflowEngineExtension> byVersion = new LinkedHashMap<>();
    byVersion.put(AirflowEngineVersions.V2, airflow2Extension);
    byVersion.put(AirflowEngineVersions.V2_LEGACY_ALIAS, airflow2Extension);

    if (AirflowEngineVersions.V3.equals(airflowEngineVersionProvider.getConfiguredVersion())
        && isAirflow3BackendConfigured()) {
      byVersion.put(
          AirflowEngineVersions.V3, new AirflowV3WorkflowEngineExtension(airflow3ApiClient()));
    }
    return new InternalAirflowExtensions(byVersion, airflow2Extension);
  }

  private boolean isAirflow3BackendConfigured() {
    return airflow3Url != null && !airflow3Url.isBlank();
  }

  /**
   * Fail-fast when Airflow 3 is selected but its backend URL or credentials are missing. Without
   * this the service would silently build an Airflow 3 ({@code api/v2}) engine pointed at the
   * Airflow 2 host/client (missing URL) or fail late at {@code /auth/token} (missing credentials),
   * instead of surfacing the misconfiguration at startup.
   */
  private void requireAirflow3BackendConfigured() {
    if (!isAirflow3BackendConfigured()) {
      throw new IllegalStateException(
          "osdu.airflow.version=airflow3 but osdu.airflow.airflow3.url is not set. Set "
              + "osdu.airflow.airflow3.url (env OSDU_AIRFLOW_AIRFLOW3_URL) to enable Airflow 3, or "
              + "set osdu.airflow.version=airflow2.");
    }
    if (airflow3Username == null || airflow3Username.isBlank()
        || airflow3Password == null || airflow3Password.isBlank()) {
      throw new IllegalStateException(
          "osdu.airflow.version=airflow3 requires osdu.airflow.airflow3.username and "
              + "osdu.airflow.airflow3.password (env OSDU_AIRFLOW_AIRFLOW3_USERNAME / "
              + "OSDU_AIRFLOW_AIRFLOW3_PASSWORD) for the api/v2 JWT token exchange.");
    }
  }
}
