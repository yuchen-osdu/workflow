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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.aws.service.mwaa.MwaaInvokeRestApiClient;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.mwaa.MwaaClient;

@ExtendWith(MockitoExtension.class)
class MwaaSigV4ApiClientServiceProviderTest {

  private static final String MWAA_SIG_V4 = "mwaaSigV4";
  private static final String WRONG_AUTH_TYPE = "BasicAuth";
  private static final String ENVIRONMENT_NAME = "mwaa10-mwaa";

  @Mock
  private MwaaClient mwaaClient;

  @InjectMocks
  private MwaaSigV4ApiClientServiceProvider provider;

  @Test
  void should_DoSupport_when_AuthTypeIsMwaaSigV4CaseInsensitive() {
    assertThat(provider.supports(MWAA_SIG_V4)).isTrue();
    assertThat(provider.supports(MWAA_SIG_V4.toLowerCase())).isTrue();
    assertThat(provider.supports(MWAA_SIG_V4.toUpperCase())).isTrue();
  }

  @Test
  void should_NotSupport_when_AuthTypeIsIncorrect() {
    assertThat(provider.supports(WRONG_AUTH_TYPE)).isFalse();
    assertThat(provider.supports("")).isFalse();
    assertThat(provider.supports(null)).isFalse();
  }

  @Test
  void should_CreateMwaaInvokeRestApiClient_when_AirflowConfigIsProvided() {
    // given
    AirflowConfig airflowConfig = new AirflowConfig();
    airflowConfig.setUrl(ENVIRONMENT_NAME);

    // when
    IAirflowApiClient apiClient = provider.create(airflowConfig);

    // then
    assertThat(apiClient).isInstanceOf(MwaaInvokeRestApiClient.class);
    assertThat(ReflectionTestUtils.getField(apiClient, "mwaaClient")).isSameAs(mwaaClient);
    assertThat(ReflectionTestUtils.getField(apiClient, "environmentName"))
        .isEqualTo(ENVIRONMENT_NAME);
  }
}
