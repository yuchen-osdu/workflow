/*
 *  Copyright 2020-2026 Google LLC
 *  Copyright 2020-2026 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.AirflowV3WorkflowEngineExtension;
import org.opengroup.osdu.workflow.service.InternalAirflowExtensions;
import org.opengroup.osdu.workflow.service.PerRunAirflowWorkflowEngineService;
import org.opengroup.osdu.workflow.service.factory.AirflowApiClientFactory;
import org.opengroup.osdu.workflow.service.factory.ExternalAirflowConfigFactory;
import org.opengroup.osdu.workflow.service.factory.WorkflowEngineServiceFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceProviderTest {

  @Mock
  private IAirflowApiClient airflowApiClient;
  @Mock
  private WorkflowEngineServiceFactory workflowEngineServiceFactory;
  @Mock
  private AirflowApiClientFactory airflowApiClientFactory;
  @Mock
  private ExternalAirflowConfigFactory externalAirflowConfigFactory;
  @Mock
  private AirflowEngineVersionProvider airflowEngineVersionProvider;

  private WorkflowEngineServiceProvider provider;

  @BeforeEach
  void setUp() {
    provider =
        new WorkflowEngineServiceProvider(
            airflowApiClient,
            workflowEngineServiceFactory,
            airflowApiClientFactory,
            externalAirflowConfigFactory,
            airflowEngineVersionProvider);
  }

  @Test
  void workflowEngineService_whenAirflow2_createsSingleEngine() {
    when(airflowEngineVersionProvider.getConfiguredVersion()).thenReturn(AirflowEngineVersions.V2);
    IWorkflowEngineService mockEngine = mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(AirflowEngineVersions.V2, airflowApiClient))
        .thenReturn(mockEngine);

    IWorkflowEngineService result = provider.workflowEngineService();

    assertThat(result).isSameAs(mockEngine);
    verify(workflowEngineServiceFactory).createWorkflowEngineService(AirflowEngineVersions.V2, airflowApiClient);
  }

  @Test
  void workflowEngineService_whenAirflow3AndNoSideBySideUrl_createsSingleAirflow3Engine() {
    when(airflowEngineVersionProvider.getConfiguredVersion()).thenReturn(AirflowEngineVersions.V3);
    ReflectionTestUtils.setField(provider, "airflow3Url", "");
    IWorkflowEngineService mockEngine = mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(AirflowEngineVersions.V3, airflowApiClient))
        .thenReturn(mockEngine);

    IWorkflowEngineService result = provider.workflowEngineService();

    assertThat(result).isSameAs(mockEngine);
    verify(workflowEngineServiceFactory).createWorkflowEngineService(AirflowEngineVersions.V3, airflowApiClient);
  }

  @Test
  void workflowEngineService_whenAirflow3AndSideBySideUrlSet_buildsPerRunEngine() {
    when(airflowEngineVersionProvider.getConfiguredVersion()).thenReturn(AirflowEngineVersions.V3);
    ReflectionTestUtils.setField(provider, "airflow3Url", "https://airflow3.example.com");
    ReflectionTestUtils.setField(provider, "airflow3Username", "admin");
    ReflectionTestUtils.setField(provider, "airflow3Password", "secret");

    IAirflowApiClient af3Client = mock(IAirflowApiClient.class);
    when(airflowApiClientFactory.createAirflowApiClient(eq("JwtAuth"), any())).thenReturn(af3Client);

    IWorkflowEngineService af2Engine = mock(IWorkflowEngineService.class);
    IWorkflowEngineService af3Engine = mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(AirflowEngineVersions.V2, airflowApiClient))
        .thenReturn(af2Engine);
    when(workflowEngineServiceFactory.createWorkflowEngineService(AirflowEngineVersions.V3, af3Client))
        .thenReturn(af3Engine);

    IWorkflowEngineService result = provider.workflowEngineService();

    assertThat(result).isInstanceOf(PerRunAirflowWorkflowEngineService.class);
  }

  @Test
  void workflowEngineService_whenAirflow3AndSideBySideUrlSetWithoutCredentials_throwsException() {
    when(airflowEngineVersionProvider.getConfiguredVersion()).thenReturn(AirflowEngineVersions.V3);
    ReflectionTestUtils.setField(provider, "airflow3Url", "https://airflow3.example.com");
    ReflectionTestUtils.setField(provider, "airflow3Username", "");
    ReflectionTestUtils.setField(provider, "airflow3Password", "");

    assertThatThrownBy(() -> provider.workflowEngineService())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("osdu.airflow.airflow3.url is configured for side-by-side Airflow 3 migration");
  }

  @Test
  void internalAirflowExtensions_whenAirflow3SingleHost_configuresAf3WithDefaultClient() {
    when(airflowEngineVersionProvider.getConfiguredVersion()).thenReturn(AirflowEngineVersions.V3);
    ReflectionTestUtils.setField(provider, "airflow3Url", "");

    InternalAirflowExtensions extensions = provider.internalAirflowExtensions();

    assertThat(extensions).isNotNull();
    assertThat(extensions.hasVersionedRouting()).isTrue();
    IWorkflowEngineExtension af3Ext = extensions.forEngineVersion(AirflowEngineVersions.V3);
    assertThat(af3Ext).isInstanceOf(AirflowV3WorkflowEngineExtension.class);
    assertThat(ReflectionTestUtils.getField(af3Ext, "airflowApiClient")).isSameAs(airflowApiClient);
    verifyNoInteractions(airflowApiClientFactory);
  }

  @Test
  void internalAirflowExtensions_whenAirflow3SideBySide_configuresAf3WithJwtClient() {
    when(airflowEngineVersionProvider.getConfiguredVersion()).thenReturn(AirflowEngineVersions.V3);
    ReflectionTestUtils.setField(provider, "airflow3Url", "https://airflow3.example.com");
    ReflectionTestUtils.setField(provider, "airflow3Username", "admin");
    ReflectionTestUtils.setField(provider, "airflow3Password", "secret");

    IAirflowApiClient af3JwtClient = mock(IAirflowApiClient.class);
    when(airflowApiClientFactory.createAirflowApiClient(eq("JwtAuth"), any())).thenReturn(af3JwtClient);

    InternalAirflowExtensions extensions = provider.internalAirflowExtensions();

    assertThat(extensions).isNotNull();
    assertThat(extensions.hasVersionedRouting()).isTrue();
    IWorkflowEngineExtension af3Ext = extensions.forEngineVersion(AirflowEngineVersions.V3);
    assertThat(af3Ext).isInstanceOf(AirflowV3WorkflowEngineExtension.class);
    assertThat(ReflectionTestUtils.getField(af3Ext, "airflowApiClient")).isSameAs(af3JwtClient);
    verify(airflowApiClientFactory).createAirflowApiClient(eq("JwtAuth"), any());
  }
}
