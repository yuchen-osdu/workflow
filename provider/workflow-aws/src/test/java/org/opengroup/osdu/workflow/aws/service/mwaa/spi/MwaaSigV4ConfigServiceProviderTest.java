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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.config.AirflowConfig;

class MwaaSigV4ConfigServiceProviderTest {

  private static final String URL = "url";
  private static final String MWAA_SIG_V4 = "mwaaSigV4";
  private static final String WRONG_AUTH_TYPE = "BasicAuth";
  private static final String ENVIRONMENT_NAME = "mwaa10-mwaa";

  private final MwaaSigV4ConfigServiceProvider provider = new MwaaSigV4ConfigServiceProvider();

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
  void should_CreateAirflowConfig_when_ConfigMapIsValid() {
    // given
    Map<String, Object> configMap = new HashMap<>();
    configMap.put(URL, ENVIRONMENT_NAME);

    // when
    AirflowConfig result = provider.create(configMap);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getUrl()).isEqualTo(ENVIRONMENT_NAME);
    assertThat(result.getUsername()).isNull();
    assertThat(result.getPassword()).isNull();
  }
}
