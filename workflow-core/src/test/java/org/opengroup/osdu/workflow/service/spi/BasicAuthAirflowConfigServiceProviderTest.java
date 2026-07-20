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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.opengroup.osdu.workflow.service.spi.BasicAuthAirflowConfigServiceProvider.ALLOW_HTTP_FF_NAME;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;

@ExtendWith(MockitoExtension.class)
class BasicAuthAirflowConfigServiceProviderTest {

  private static final String URL = "url";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String TEST_URL = "https://url.com";
  private static final String HTTP_TEST_URL = "http://url.com";
  private static final String TEST_USERNAME = "testuser";
  private static final String TEST_PASSWORD = "s3cret";
  private static final String BASIC_AUTH1 = "BasicAuth";
  private static final String BASIC_AUTH2 = "basicauth";
  private static final String BASIC_AUTH3 = "BASICAUTH";
  private static final String OTHER_AUTH = "OtherAuth";

  @Mock
  private IFeatureFlag featureFlag;
  @InjectMocks
  private BasicAuthAirflowConfigServiceProvider provider;

  @Test
  void should_ReturnTrue_when_SupportsBasicAuthTypeCaseInsensitive() {
    assertThat(provider.supports(BASIC_AUTH1)).isTrue();
    assertThat(provider.supports(BASIC_AUTH2)).isTrue();
    assertThat(provider.supports(BASIC_AUTH3)).isTrue();
  }

  @Test
  void should_ReturnFalse_when_SupportsNotBasicAuthType() {
    assertThat(provider.supports(null)).isFalse();
    assertThat(provider.supports("")).isFalse();
    assertThat(provider.supports(OTHER_AUTH)).isFalse();
  }

  @Test
  void should_CreateAirflowConfig_when_ConfigMapIsValid() {
    Map<String, Object> validConfigMap = getValidConfigMap();
    AirflowConfig result = provider.create(validConfigMap);
    assertThat(result).isNotNull();
    assertEquals(TEST_URL, result.getUrl());
    assertEquals(TEST_USERNAME, result.getUsername());
    assertEquals(TEST_PASSWORD, result.getPassword());
  }

  @Test
  void should_ThrowAppException_when_UrlIsMissingInConfig() {
    Map<String, Object> configMap = getValidConfigMap();
    configMap.remove(URL);
    AppException ex = assertThrows(AppException.class, () -> provider.create(configMap));
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.getError().getCode());
    assertThat(ex.getError().getMessage()).contains("not found");
    assertThat(ex.getError().getMessage()).contains(URL);
  }

  @Test
  void should_ThrowAppException_when_UsernameIsMissingInConfig() {
    Map<String, Object> configMap = getValidConfigMap();
    configMap.remove(USERNAME);
    AppException ex = assertThrows(AppException.class, () -> provider.create(configMap));
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.getError().getCode());
    assertThat(ex.getError().getMessage()).contains("not found");
    assertThat(ex.getError().getMessage()).contains(USERNAME);
  }

  @Test
  void should_ThrowAppException_when_PasswordIsMissingInConfig() {
    Map<String, Object> configMap = getValidConfigMap();
    configMap.remove(PASSWORD);
    AppException ex = assertThrows(AppException.class, () -> provider.create(configMap));
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.getError().getCode());
    assertThat(ex.getError().getMessage()).contains("not found");
    assertThat(ex.getError().getMessage()).contains(PASSWORD);
  }

  @Test
  void should_ThrowAppException_when_httpUrlAndHttpIsNotAllowed() {
    Map<String, Object> configMap = getValidConfigMap();
    configMap.put(URL, HTTP_TEST_URL);
    AppException ex = assertThrows(AppException.class, () -> provider.create(configMap));
    assertEquals(HttpStatus.SC_BAD_REQUEST, ex.getError().getCode());
  }

  @Test
  void shouldNot_ThrowAppException_when_httpUrlAndHttpIsAllowed() {
    when(featureFlag.isFeatureEnabled(ALLOW_HTTP_FF_NAME)).thenReturn(true);
    Map<String, Object> configMap = getValidConfigMap();
    configMap.put(URL, HTTP_TEST_URL);
    AirflowConfig result = provider.create(configMap);
    assertThat(result).isNotNull();
    assertEquals(HTTP_TEST_URL, result.getUrl());
    assertEquals(TEST_USERNAME, result.getUsername());
    assertEquals(TEST_PASSWORD, result.getPassword());
  }

  private Map<String, Object> getValidConfigMap() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put(URL, TEST_URL);
    configMap.put(USERNAME, TEST_USERNAME);
    configMap.put(PASSWORD, TEST_PASSWORD);
    return configMap;
  }
}
