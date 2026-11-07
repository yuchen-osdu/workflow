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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.AirflowV3WorkflowEngineServiceImpl;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AirflowV3WorkflowEngineServiceProvider implements IWorkflowEngineServiceProvider {

  private final DpsHeaders dpsHeaders;

  @Override
  public boolean supports(String version) {
    log.debug("Checking support for version: {}", version);
    return AirflowEngineVersions.V3.equalsIgnoreCase(version)
        || AirflowEngineVersions.V3_SHORT_ALIAS.equalsIgnoreCase(version);
  }

  @Override
  public IWorkflowEngineService create(IAirflowApiClient airflowApiClient) {
    log.info("Creating Airflow V3 workflow engine service");
    return new AirflowV3WorkflowEngineServiceImpl(airflowApiClient, dpsHeaders);
  }
}
