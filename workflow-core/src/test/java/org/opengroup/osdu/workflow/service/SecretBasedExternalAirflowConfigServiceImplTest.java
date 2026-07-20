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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.secret.AccessGroups;
import org.opengroup.osdu.core.common.secret.SecretClient;
import org.opengroup.osdu.core.common.secret.SecretClientFactory;
import org.opengroup.osdu.core.common.secret.SecretModel;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.workflow.model.ExternalAirflowConfig;

@ExtendWith(MockitoExtension.class)
class SecretBasedExternalAirflowConfigServiceImplTest {

  private static final String EXTERNAL_AIRFLOW_SECRET_ID = "secret-id";
  private static final String AIRFLOW_VERSION = "2.2.2";
  private static final String API_CLIENT_TYPE = "api-client-type";
  private static final String SECRET_VALUE_JSON =
      "{\"version\":\"2.2.2\",\"airflowApiClientType\":\"api-client-type\",\"some-key\":\"some-value\"}";
  private static final String SECRET_VALUE_JSON_WITHOUT_VERSION =
      "{\"airflowApiClientType\":\"api-client-type\",\"some-key\":\"some-value\"}";
  private static final String SECRET_VALUE_JSON_WITHOUT_API_CLIENT_TYPE =
      "{\"version\":\"2.2.2\",\"some-key\":\"some-value\"}";
  private static final String INVALID_SECRET_VALUE_JSON = "invalid-json";

  @Mock private SecretClientFactory secretFactory;
  @Mock private IServiceAccountJwtClient tokenService;
  @Mock private DpsHeaders dpsHeaders;
  @Mock private AccessGroups accessGroups;
  @Mock private SecretClient secretClient;
  @Mock private SecretModel secretModel;

  @InjectMocks
  private SecretBasedExternalAirflowConfigServiceImpl secretBasedExternalAirflowConfigService;

  @Test
  void should_returnExternalAirflowConfig_when_validSecretIsRetrieved() {
    // given
    when(secretFactory.create(dpsHeaders, tokenService, accessGroups)).thenReturn(secretClient);
    when(secretClient.retrieveSecret(EXTERNAL_AIRFLOW_SECRET_ID)).thenReturn(secretModel);
    when(secretModel.getValue()).thenReturn(SECRET_VALUE_JSON);

    // when
    ExternalAirflowConfig result =
        secretBasedExternalAirflowConfigService.getExternalAirflowConfig(
            EXTERNAL_AIRFLOW_SECRET_ID);

    // then
    assertThat(result.getAirflowVersion()).isEqualTo(AIRFLOW_VERSION);
    assertThat(result.getAirflowApiClientType()).isEqualTo(API_CLIENT_TYPE);
    assertThat(result.getConfigMap()).containsEntry("some-key", "some-value");
  }

  @Test
  void should_throwAppException_when_secretValueIsValidJsonWithoutVersion() {
    // given
    when(secretFactory.create(dpsHeaders, tokenService, accessGroups)).thenReturn(secretClient);
    when(secretClient.retrieveSecret(EXTERNAL_AIRFLOW_SECRET_ID)).thenReturn(secretModel);
    when(secretModel.getValue()).thenReturn(SECRET_VALUE_JSON_WITHOUT_VERSION);

    // when & then
    assertThrows(
        AppException.class,
        () ->
            secretBasedExternalAirflowConfigService.getExternalAirflowConfig(
                EXTERNAL_AIRFLOW_SECRET_ID));
  }

  @Test
  void should_throwAppException_when_secretValueIsValidJsonWithoutApiClientType() {
    // given
    when(secretFactory.create(dpsHeaders, tokenService, accessGroups)).thenReturn(secretClient);
    when(secretClient.retrieveSecret(EXTERNAL_AIRFLOW_SECRET_ID)).thenReturn(secretModel);
    when(secretModel.getValue()).thenReturn(SECRET_VALUE_JSON_WITHOUT_API_CLIENT_TYPE);

    // when & then
    assertThrows(
        AppException.class,
        () ->
            secretBasedExternalAirflowConfigService.getExternalAirflowConfig(
                EXTERNAL_AIRFLOW_SECRET_ID));
  }

  @Test
  void should_throwAppException_when_secretValueIsInvalidJson() {
    // given
    when(secretFactory.create(dpsHeaders, tokenService, accessGroups)).thenReturn(secretClient);
    when(secretClient.retrieveSecret(EXTERNAL_AIRFLOW_SECRET_ID)).thenReturn(secretModel);
    when(secretModel.getValue()).thenReturn(INVALID_SECRET_VALUE_JSON);

    // when & then
    assertThrows(
        AppException.class,
        () ->
            secretBasedExternalAirflowConfigService.getExternalAirflowConfig(
                EXTERNAL_AIRFLOW_SECRET_ID));
  }
}
