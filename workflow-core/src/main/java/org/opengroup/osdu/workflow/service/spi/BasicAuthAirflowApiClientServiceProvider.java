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
import org.opengroup.osdu.workflow.service.BasicAuthAirflowApiClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BasicAuthAirflowApiClientServiceProvider implements IAirflowApiClientServiceProvider {

  private static final String BASIC_AUTH = "BasicAuth";

  private final Client restClient;

  @Override
  public boolean supports(String airflowApiClientType) {
    log.debug("Checking support for Airflow API client type: {}", airflowApiClientType);
    return BASIC_AUTH.equalsIgnoreCase(airflowApiClientType);
  }

  @Override
  public IAirflowApiClient create(AirflowConfig airflowConfig) {
    log.info("Creating BasicAuth Airflow API client with config: {}", airflowConfig);
    return new BasicAuthAirflowApiClient(restClient, airflowConfig);
  }
}
