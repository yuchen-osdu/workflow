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

package org.opengroup.osdu.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.secret.AccessGroups;
import org.opengroup.osdu.core.common.secret.SecretClient;
import org.opengroup.osdu.core.common.secret.SecretClientFactory;
import org.opengroup.osdu.core.common.secret.SecretModel;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.workflow.model.ExternalAirflowConfig;
import org.opengroup.osdu.workflow.provider.interfaces.IExternalAirflowConfigService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretBasedExternalAirflowConfigServiceImpl implements IExternalAirflowConfigService {
  private static final String VERSION = "version";
  private static final String AIRFLOW_API_CLIENT_TYPE = "airflowApiClientType";
  private final SecretClientFactory secretFactory;
  private final IServiceAccountJwtClient tokenService;
  private final DpsHeaders dpsHeaders;
  private final AccessGroups accessGroups;

  @Override
  public ExternalAirflowConfig getExternalAirflowConfig(String externalAirflowSecretId) {
    log.info("Getting external airflow config for secret id: {}", externalAirflowSecretId);
    ExternalAirflowConfig externalAirflowConfig = new ExternalAirflowConfig();

    SecretClient secretClient = secretFactory.create(dpsHeaders, tokenService, accessGroups);
    SecretModel secret;
    try {
      secret = secretClient.retrieveSecret(externalAirflowSecretId);
      log.debug(
          "Successfully retrieved secret for external airflow config: {}", externalAirflowSecretId);
    } catch (AppException e) {
      log.error(
          "Error retrieving secret for external airflow config: {}", externalAirflowSecretId, e);
      throw new AppException(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          "Error retrieving external airflow config",
          "Cannot retrieve secret for external airflow %s".formatted(externalAirflowSecretId),
          e);
    }
    String secretValue = secret.getValue();

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      Map<String, Object> configMap = objectMapper.readValue(secretValue, new TypeReference<>() {});
      externalAirflowConfig.setConfigMap(configMap);
    } catch (JsonProcessingException e) {
      log.error("Error parsing secret for external airflow config: {}", externalAirflowSecretId, e);
      throw new AppException(
          HttpStatus.SC_BAD_REQUEST,
          "Error reading external airflow config",
          "Cannot parse secret for external airflow %s",
          externalAirflowSecretId,
          e);
    }
    externalAirflowConfig.setAirflowVersion(externalAirflowConfig.getStringValue(VERSION));
    externalAirflowConfig.setAirflowApiClientType(
        externalAirflowConfig.getStringValue(AIRFLOW_API_CLIENT_TYPE));

    log.info(
        "Successfully retrieved external airflow config for secret id: {}",
        externalAirflowSecretId);
    return externalAirflowConfig;
  }
}
