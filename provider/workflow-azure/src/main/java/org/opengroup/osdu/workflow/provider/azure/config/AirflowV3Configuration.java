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

package org.opengroup.osdu.workflow.provider.azure.config;

import java.util.Locale;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the {@code airflow3Config} bean holding the Airflow 3 backend connection. Active only
 * when Airflow 3 is enabled ({@code osdu.airflow.version=airflow3}).
 *
 * <p>{@link ConfigurationProperties} is placed on the {@code @Bean} method (not the
 * {@link AirflowConfig} class) so Spring binds the {@code osdu.airflow.airflow3.*} prefix onto THIS
 * bean only. The class-level {@code @ConfigurationProperties("osdu.airflow")} on {@link AirflowConfig}
 * still governs the auto-registered {@code airflowConfig} (Airflow 2) bean; without this override the
 * post-processor would rebind the Airflow 2 URL onto the Airflow 3 bean.
 */
@Configuration
@ConditionalOnExpression(
    "T(org.opengroup.osdu.workflow.model.AirflowEngineVersions).isAirflow3('${osdu.airflow.version:airflow2}')")
@EnableConfigurationProperties(AirflowV3Properties.class)
public class AirflowV3Configuration {

  @Bean(name = "airflow3Config")
  @ConfigurationProperties("osdu.airflow.airflow3")
  public AirflowConfig airflow3Config(AirflowV3Properties props) {
    validateUrl(props.getUrl());
    validateCredentials(props.getUsername(), props.getPassword());
    return new AirflowConfig();
  }

  private void validateCredentials(String username, String password) {
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      throw new IllegalStateException(
          "osdu.airflow.airflow3.username and osdu.airflow.airflow3.password are required when "
              + "Airflow 3 is enabled (for the api/v2 JWT token exchange).");
    }
  }

  private void validateUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalStateException(
          "osdu.airflow.airflow3.url is required when Airflow 3 is enabled "
              + "(osdu.airflow.version=airflow3). Set it in application.properties or via env "
              + "OSDU_AIRFLOW_AIRFLOW3_URL.");
    }
    String lower = url.toLowerCase(Locale.ROOT);
    if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
      throw new IllegalStateException(
          "osdu.airflow.airflow3.url must start with http:// or https:// (got: '" + url + "').");
    }
    // No https-only enforcement: like the Airflow 2 URL, the Airflow 3 endpoint is an internal
    // in-mesh Kubernetes URL reached over http (transport secured by the service mesh). Airflow 3
    // must not impose a stricter transport constraint than Airflow 2, which is ungated.
  }
}
