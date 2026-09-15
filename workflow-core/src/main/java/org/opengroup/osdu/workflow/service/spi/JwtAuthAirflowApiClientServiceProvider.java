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

package org.opengroup.osdu.workflow.service.spi;

import com.sun.jersey.api.client.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.service.JwtAuthAirflowApiClient;
import org.springframework.stereotype.Component;

/**
 * Registers the {@link JwtAuthAirflowApiClient} under the {@code JwtAuth} client type, used for the
 * Airflow 3 {@code api/v2} backend which authenticates with JWT bearer tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthAirflowApiClientServiceProvider implements IAirflowApiClientServiceProvider {

  private static final String JWT_AUTH = "JwtAuth";

  private final Client restClient;

  @Override
  public boolean supports(String airflowApiClientType) {
    log.debug("Checking support for Airflow API client type: {}", airflowApiClientType);
    return JWT_AUTH.equalsIgnoreCase(airflowApiClientType);
  }

  @Override
  public IAirflowApiClient create(AirflowConfig airflowConfig) {
    log.info("Creating JwtAuth Airflow API client for url: {}", airflowConfig.getUrl());
    return new JwtAuthAirflowApiClient(restClient, airflowConfig);
  }
}
