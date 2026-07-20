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
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.spi.IWorkflowEngineServiceProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngineServiceFactory {

  private final List<IWorkflowEngineServiceProvider> providers;

  public IWorkflowEngineService createWorkflowEngineService(
      String airflowVersion, IAirflowApiClient airflowApiClient) {

    log.info("Creating WorkflowEngineService for Airflow version: {}", airflowVersion);
    return providers.stream()
        .filter(provider -> provider.supports(airflowVersion))
        .findFirst()
        .map(provider -> provider.create(airflowApiClient))
        .orElseThrow(
            () -> {
              log.error("Unsupported Airflow version: {}", airflowVersion);
              return new AppException(
                  HttpStatus.INTERNAL_SERVER_ERROR.value(),
                  "Unsupported Airflow version",
                  "Unsupported Airflow version: %s".formatted(airflowVersion));
            });
  }
}
