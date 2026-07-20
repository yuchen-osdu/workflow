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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BasicAuthAirflowConfigServiceProvider implements IAirflowConfigServiceProvider {

  public static final String ALLOW_HTTP_FF_NAME = "featureFlag.allow.http.airflow";
  private static final String BASIC_AUTH = "BasicAuth";
  private static final String URL = "url";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";

  private final IFeatureFlag featureFlag;

  @Override
  public boolean supports(String airflowApiClientType) {
    log.debug("Checking support for Airflow API client type: {}", airflowApiClientType);
    return BASIC_AUTH.equalsIgnoreCase(airflowApiClientType);
  }

  @Override
  public AirflowConfig create(Map<String, Object> configMap) {
    log.info("Creating Airflow config for BasicAuth");
    AirflowConfig airflowConfig = new AirflowConfig();
    String url = IAirflowConfigServiceProvider.getStringValue(URL, configMap);
    boolean httpAirflowAllowed = featureFlag.isFeatureEnabled(ALLOW_HTTP_FF_NAME);
    if (!httpAirflowAllowed && !url.toLowerCase().startsWith("https://")) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST,
          "Invalid URL",
          "External Airflow URL must use HTTPS");
    } else {
      airflowConfig.setUrl(url);
      airflowConfig.setUsername(IAirflowConfigServiceProvider.getStringValue(USERNAME, configMap));
      airflowConfig.setPassword(IAirflowConfigServiceProvider.getStringValue(PASSWORD, configMap));
      return airflowConfig;
    }
  }
}
