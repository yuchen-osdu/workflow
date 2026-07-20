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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.spi.IWorkflowEngineServiceProvider;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceFactoryTest {

  private static final String SUPPORTED_VERSION = "2.0.0";
  private static final String UNSUPPORTED_VERSION = "1.0.0";
  private static final IAirflowApiClient MOCK_AIRFLOW_API_CLIENT = mock(IAirflowApiClient.class);

  @Mock
  private IWorkflowEngineServiceProvider provider1;
  @Mock
  private IWorkflowEngineServiceProvider provider2;
  @Mock
  private IWorkflowEngineService expectedService;

  @InjectMocks
  private WorkflowEngineServiceFactory factory;

  @Test
  void should_CreateWorkflowEngineService_when_ProviderSupportsVersion() {
    // given
    when(provider1.supports(SUPPORTED_VERSION)).thenReturn(false);
    when(provider2.supports(SUPPORTED_VERSION)).thenReturn(true);
    when(provider2.create(MOCK_AIRFLOW_API_CLIENT)).thenReturn(expectedService);
    factory = new WorkflowEngineServiceFactory(List.of(provider1, provider2));

    // when
    IWorkflowEngineService result = factory
        .createWorkflowEngineService(SUPPORTED_VERSION, MOCK_AIRFLOW_API_CLIENT);

    // then
    assertThat(result).isSameAs(expectedService);
    verify(provider2).create(MOCK_AIRFLOW_API_CLIENT);
  }

  @Test
  void should_ThrowException_when_NoProviderSupportsVersion() {
    // given
    when(provider1.supports(UNSUPPORTED_VERSION)).thenReturn(false);
    when(provider2.supports(UNSUPPORTED_VERSION)).thenReturn(false);
    factory = new WorkflowEngineServiceFactory(List.of(provider1, provider2));

    // when & then
    AppException ex =
        assertThrows(
            AppException.class,
            () -> factory
                .createWorkflowEngineService(UNSUPPORTED_VERSION, MOCK_AIRFLOW_API_CLIENT));
    assertThat(ex.getError().getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(ex.getMessage()).contains("Unsupported Airflow version");
    assertThat(ex.getError().getMessage()).contains(UNSUPPORTED_VERSION);
  }
}
