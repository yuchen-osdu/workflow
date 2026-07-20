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

package org.opengroup.osdu.workflow.di;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.info.ConnectedOuterServicesBuilder;
import org.opengroup.osdu.core.common.model.info.ConnectedOuterService;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Schema(description = "Connected outer service information.")
public class CloudConnectedOuterServicesBuilder implements ConnectedOuterServicesBuilder {

  private final IAirflowResolver airflowResolver;

  @Override
  public List<ConnectedOuterService> buildConnectedOuterServices() {
    return airflowResolver.getConnectedWorkflowEngineServicesVersions();
  }
}
