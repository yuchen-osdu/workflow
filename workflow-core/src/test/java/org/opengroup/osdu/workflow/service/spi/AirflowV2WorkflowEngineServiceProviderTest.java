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
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;

@ExtendWith(MockitoExtension.class)
class AirflowV2WorkflowEngineServiceProviderTest {

  private static final String V2 = "v2";
  private static final String WRONG_VERSION = "v1";

  @Mock
  private IAirflowApiClient airflowApiClient;

  @InjectMocks
  private AirflowV2WorkflowEngineServiceProvider provider;

  @Test
  void should_DoSupport_when_VersionIsV2CaseInsensitive() {
    assertThat(provider.supports(V2)).isTrue();
    assertThat(provider.supports(V2.toLowerCase())).isTrue();
    assertThat(provider.supports(V2.toUpperCase())).isTrue();
  }

  @Test
  void should_NotSupport_when_VersionIsIncorrect() {
    assertThat(provider.supports(WRONG_VERSION)).isFalse();
    assertThat(provider.supports("")).isFalse();
    assertThat(provider.supports(null)).isFalse();
  }

  @Test
  void should_CreateWorkflowEngineService_when_ApiClientIsProvided() {
    // when
    IWorkflowEngineService workflowEngineService = provider.create(airflowApiClient);

    // then
    assertThat(workflowEngineService).isNotNull();
  }
}
