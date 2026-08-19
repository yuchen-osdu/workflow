/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.workflow.aws.service.mwaa.spi;

import java.util.Map;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.service.spi.IAirflowConfigServiceProvider;
import org.springframework.stereotype.Component;

/**
 * Builds the AirflowConfig for MWAA SigV4 authentication. Needed because MWAA authenticates via
 * IAM SigV4, not JWT/Basic auth, so no username/password is set here; the environment name is
 * carried in the reused "url" field since AirflowConfig has no dedicated field for it.
 */
@Component
public class MwaaSigV4ConfigServiceProvider implements IAirflowConfigServiceProvider {

  private static final String URL = "url";

  @Override
  public boolean supports(String airflowApiClientType) {
    return MwaaSigV4Constants.MWAA_SIG_V4.equalsIgnoreCase(airflowApiClientType);
  }

  @Override
  public AirflowConfig create(Map<String, Object> configMap) {
    AirflowConfig airflowConfig = new AirflowConfig();
    // "url" here holds the MWAA environment name, not a URL — see class javadoc.
    airflowConfig.setUrl(IAirflowConfigServiceProvider.getStringValue(URL, configMap));
    return airflowConfig;
  }
}
