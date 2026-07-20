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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;

@ExtendWith(MockitoExtension.class)
class BasicAuthAirflowApiClientServiceProviderTest {

  private static final String BASIC_AUTH = "BasicAuth";
  private static final String WRONG_AUTH_TYPE = "JWT";

  @Mock
  private AirflowConfig airflowConfig;

  @InjectMocks
  private BasicAuthAirflowApiClientServiceProvider provider;

  @Test
  void should_DoSupport_when_AuthTypeIsBasicAuthCaseInsensitive() {
    assertThat(provider.supports(BASIC_AUTH)).isTrue();
    assertThat(provider.supports(BASIC_AUTH.toLowerCase())).isTrue();
    assertThat(provider.supports(BASIC_AUTH.toUpperCase())).isTrue();
  }

  @Test
  void should_NotSupport_when_AuthTypeIsIncorrect() {
    assertThat(provider.supports(WRONG_AUTH_TYPE)).isFalse();
    assertThat(provider.supports("")).isFalse();
    assertThat(provider.supports(null)).isFalse();
  }

  @Test
  void should_CreateAirflowApiClient_when_AirflowConfigIsProvided() {
    // when
    IAirflowApiClient apiClient = provider.create(airflowConfig);

    // then
    assertThat(apiClient).isNotNull();
  }
}
