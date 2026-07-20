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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.service.spi.IAirflowConfigServiceProvider;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ExternalAirflowConfigFactoryTest {

  private static final String SUPPORTED_TYPE = "supportedType";
  private static final String UNSUPPORTED_TYPE = "unsupportedType";
  private static final Map<String, Object> CONFIG_MAP = Collections.emptyMap();

  @Mock
  private IAirflowConfigServiceProvider provider1;
  @Mock
  private IAirflowConfigServiceProvider provider2;
  @Mock
  private AirflowConfig expectedConfig;

  @InjectMocks
  private ExternalAirflowConfigFactory factory;

  @Test
  void should_CreateExternalAirflowConfig_when_ProviderSupportsType() {
    // given
    when(provider1.supports(SUPPORTED_TYPE)).thenReturn(false);
    when(provider2.supports(SUPPORTED_TYPE)).thenReturn(true);
    when(provider2.create(CONFIG_MAP)).thenReturn(expectedConfig);
    factory = new ExternalAirflowConfigFactory(List.of(provider1, provider2));

    // when
    AirflowConfig result = factory.createExternalAirflowConfig(SUPPORTED_TYPE, CONFIG_MAP);

    // then
    assertThat(result).isSameAs(expectedConfig);
    verify(provider2).create(CONFIG_MAP);
  }

  @Test
  void should_ThrowException_when_NoProviderSupportsType() {
    // given
    when(provider1.supports(UNSUPPORTED_TYPE)).thenReturn(false);
    when(provider2.supports(UNSUPPORTED_TYPE)).thenReturn(false);
    factory = new ExternalAirflowConfigFactory(List.of(provider1, provider2));

    // when & then
    AppException ex =
        assertThrows(
            AppException.class,
            () -> factory.createExternalAirflowConfig(UNSUPPORTED_TYPE, CONFIG_MAP));
    assertThat(ex.getError().getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(ex.getMessage()).contains("Unsupported Airflow API client type");
    assertThat(ex.getError().getMessage()).contains(UNSUPPORTED_TYPE);
  }
}
