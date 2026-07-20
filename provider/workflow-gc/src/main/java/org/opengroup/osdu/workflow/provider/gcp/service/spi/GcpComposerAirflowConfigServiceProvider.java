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

package org.opengroup.osdu.workflow.provider.gcp.service.spi;

import java.util.Map;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.service.spi.IAirflowConfigServiceProvider;
import org.springframework.stereotype.Component;

@Component
public class GcpComposerAirflowConfigServiceProvider implements IAirflowConfigServiceProvider {

  private static final String GCP_COMPOSER = "gcpComposer";
  private static final String URL = "url";

  @Override
  public boolean supports(String airflowApiClientType) {
    return GCP_COMPOSER.equalsIgnoreCase(airflowApiClientType);
  }

  @Override
  public AirflowConfig create(Map<String, Object> configMap) {
    AirflowConfig airflowConfig = new AirflowConfig();
    airflowConfig.setUrl(IAirflowConfigServiceProvider.getStringValue(URL, configMap));
    return airflowConfig;
  }
}
