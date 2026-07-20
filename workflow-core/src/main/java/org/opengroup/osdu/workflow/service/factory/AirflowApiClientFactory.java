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

package org.opengroup.osdu.workflow.service.factory;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.service.spi.IAirflowApiClientServiceProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirflowApiClientFactory {

  private final List<IAirflowApiClientServiceProvider> providers;

  public IAirflowApiClient createAirflowApiClient(
      String airflowApiClientType, AirflowConfig airflowConfig) {

    log.info("Creating airflow api client for type: {}", airflowApiClientType);
    return providers.stream()
        .filter(provider -> provider.supports(airflowApiClientType))
        .findFirst()
        .map(provider -> provider.create(airflowConfig))
        .orElseThrow(
            () -> {
              log.error("Unsupported Airflow API client type: {}", airflowApiClientType);
              return new AppException(
                  HttpStatus.INTERNAL_SERVER_ERROR.value(),
                  "Unsupported Airflow API client type",
                  "Unsupported Airflow API client type: %s".formatted(airflowApiClientType));
            });
  }
}
