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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.info.ConnectedOuterService;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ExternalAirflowConfig;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IExternalAirflowConfigService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.factory.AirflowApiClientFactory;
import org.opengroup.osdu.workflow.service.factory.ExternalAirflowConfigFactory;
import org.opengroup.osdu.workflow.service.factory.WorkflowEngineExtensionServiceFactory;
import org.opengroup.osdu.workflow.service.factory.WorkflowEngineServiceFactory;

@ExtendWith(MockitoExtension.class)
class AirflowResolverImplTest {

  private static final String SECRET_ID = "secret-id";
  private static final String EXTERNAL_AIRFLOW_SECRET = "externalAirflowSecret";
  private static final String API_CLIENT_TYPE = "api-client-type";
  private static final String INTERNAL_AIRFLOW_VERSION = "internal-airflow-version";
  private static final String EXTERNAL_AIRFLOW_VERSION = "external-airflow-version";
  private static final String INTERNAL_AIRFLOW = "Internal Airflow";
  private static final String EXTERNAL_AIRFLOW = "External Airflow: ";
  private static final String N_A = "N/A";

  @Mock private IWorkflowEngineService internalWorkflowEngineService;
  @Mock private IWorkflowEngineExtension internalWorkflowEngineExtension;
  @Mock private WorkflowEngineServiceFactory workflowEngineServiceFactory;
  @Mock private WorkflowEngineExtensionServiceFactory workflowEngineExtensionServiceFactory;
  @Mock private AirflowApiClientFactory airflowApiClientFactory;
  @Mock private ExternalAirflowConfigFactory externalAirflowConfigFactory;
  @Mock private IExternalAirflowConfigService externalAirflowConfigService;
  @Mock private IAirflowApiClient airflowApiClient;

  @InjectMocks private AirflowResolverImpl airflowResolver;

  @Test
  void should_returnInternalAirflowVersion_when_noExternalAirflows() {
    // given
    when(internalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(INTERNAL_AIRFLOW_VERSION));

    // when
    List<ConnectedOuterService> result =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(INTERNAL_AIRFLOW);
    assertThat(result.get(0).getVersion()).isEqualTo(INTERNAL_AIRFLOW_VERSION);
  }

  @Test
  void should_returnInternalAndExternalAirflowVersions_when_externalAirflowExist() {
    // given
    when(internalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(INTERNAL_AIRFLOW_VERSION));

    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    when(externalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(EXTERNAL_AIRFLOW_VERSION));
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    when(workflowEngineServiceFactory.createWorkflowEngineService(
        EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService);

    // when
    airflowResolver.getWorkflowEngineService(workflowMetadata);
    List<ConnectedOuterService> result =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo(INTERNAL_AIRFLOW);
    assertThat(result.get(0).getVersion()).isEqualTo(INTERNAL_AIRFLOW_VERSION);
    assertThat(result.get(1).getName()).isEqualTo(EXTERNAL_AIRFLOW + SECRET_ID);
    assertThat(result.get(1).getVersion()).isEqualTo(EXTERNAL_AIRFLOW_VERSION);
  }

  @Test
  void should_returnNotAvailable_when_internalAirflowVersionIsEmpty() {
    // given
    when(internalWorkflowEngineService.getVersion()).thenReturn(Optional.empty());

    // when
    List<ConnectedOuterService> result =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(INTERNAL_AIRFLOW);
    assertThat(result.get(0).getVersion()).isEqualTo(N_A);
  }

  @Test
  void should_returnNotAvailable_when_externalAirflowVersionIsEmpty() {
    // given
    when(internalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(INTERNAL_AIRFLOW_VERSION));

    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    when(externalWorkflowEngineService.getVersion()).thenReturn(Optional.empty());
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    when(workflowEngineServiceFactory.createWorkflowEngineService(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService);

    // when
    airflowResolver.getWorkflowEngineService(workflowMetadata);
    List<ConnectedOuterService> result =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(1).getName()).isEqualTo(EXTERNAL_AIRFLOW + SECRET_ID);
    assertThat(result.get(1).getVersion()).isEqualTo(N_A);
  }

  @Test
  void should_returnNotAvailable_when_externalAirflowSecretIsUnreachable() {
    // given
    when(internalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(INTERNAL_AIRFLOW_VERSION));

    Map<String, Object> registrationInstructions = new HashMap<>();
    registrationInstructions.put(EXTERNAL_AIRFLOW_SECRET, SECRET_ID);
    WorkflowMetadata workflowMetadata =
        WorkflowMetadata.builder().registrationInstructions(registrationInstructions).build();
    ExternalAirflowConfig externalAirflowConfig =
        ExternalAirflowConfig.builder()
            .airflowVersion(EXTERNAL_AIRFLOW_VERSION)
            .airflowApiClientType(API_CLIENT_TYPE)
            .configMap(Collections.emptyMap())
            .build();
    when(externalAirflowConfigService.getExternalAirflowConfig(SECRET_ID))
        .thenReturn(externalAirflowConfig)
        .thenThrow(new AppException(500, "Secret unavailable", "Unable to retrieve secret"));
    AirflowConfig airflowConfig = new AirflowConfig();
    when(externalAirflowConfigFactory.createExternalAirflowConfig(
            API_CLIENT_TYPE, Collections.emptyMap()))
        .thenReturn(airflowConfig);
    when(airflowApiClientFactory.createAirflowApiClient(API_CLIENT_TYPE, airflowConfig))
        .thenReturn(airflowApiClient);
    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService);

    // when
    airflowResolver.getWorkflowEngineService(workflowMetadata);
    List<ConnectedOuterService> result =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(1).getName()).isEqualTo(EXTERNAL_AIRFLOW + SECRET_ID);
    assertThat(result.get(1).getVersion()).isEqualTo(N_A);
  }

  @Test
  void should_notDuplicateExternalAirflowInInfo_when_sameSecretIsResolvedMultipleWays() {
    // given
    when(internalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(INTERNAL_AIRFLOW_VERSION));

    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    when(externalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(EXTERNAL_AIRFLOW_VERSION));
    IWorkflowEngineExtension externalWorkflowEngineExtension = mock(IWorkflowEngineExtension.class);
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    when(workflowEngineServiceFactory.createWorkflowEngineService(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService);
    when(workflowEngineExtensionServiceFactory.createWorkflowEngineExtension(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineExtension);

    // when
    airflowResolver.getWorkflowEngineService(workflowMetadata);
    airflowResolver.getWorkflowEngineExtension(workflowMetadata);
    List<ConnectedOuterService> result =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(result).hasSize(2);
    assertThat(result)
        .filteredOn(service -> service.getName().equals(EXTERNAL_AIRFLOW + SECRET_ID))
        .hasSize(1);
  }

  @Test
  void should_reuseCachedExternalAirflowVersion_when_infoIsRequestedAgain() {
    // given
    String firstInfoVersion = "first-info-version";
    when(internalWorkflowEngineService.getVersion())
        .thenReturn(Optional.of(INTERNAL_AIRFLOW_VERSION));

    IWorkflowEngineService firstInfoWorkflowEngineService = mock(IWorkflowEngineService.class);
    when(firstInfoWorkflowEngineService.getVersion()).thenReturn(Optional.of(firstInfoVersion));
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    when(workflowEngineServiceFactory.createWorkflowEngineService(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(firstInfoWorkflowEngineService);

    // when
    airflowResolver.getWorkflowEngineService(workflowMetadata);
    List<ConnectedOuterService> firstResult =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();
    List<ConnectedOuterService> secondResult =
        airflowResolver.getConnectedWorkflowEngineServicesVersions();

    // then
    assertThat(firstResult.get(1).getVersion()).isEqualTo(firstInfoVersion);
    assertThat(secondResult.get(1).getVersion()).isEqualTo(firstInfoVersion);
    verify(externalAirflowConfigService).getExternalAirflowConfig(SECRET_ID);
  }

  @Test
  void should_returnInternalWorkflowEngineService_when_noExternalAirflowSecretInMetadata() {
    // given
    WorkflowMetadata workflowMetadata =
        WorkflowMetadata.builder().registrationInstructions(Collections.emptyMap()).build();

    // when
    IWorkflowEngineService result = airflowResolver.getWorkflowEngineService(workflowMetadata);

    // then
    assertThat(result).isEqualTo(internalWorkflowEngineService);
  }

  @Test
  void should_returnInternalWorkflowEngineService_when_registrationInstructionsAreNull() {
    // given
    WorkflowMetadata workflowMetadata = WorkflowMetadata.builder().build();

    // when
    IWorkflowEngineService result = airflowResolver.getWorkflowEngineService(workflowMetadata);

    // then
    assertThat(result).isEqualTo(internalWorkflowEngineService);
  }

  @Test
  void
      should_returnExternalWorkflowEngineService_when_externalAirflowSecretIsInMetadataAndNotInCache() {
    // given
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();

    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(
        EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService);

    // when
    IWorkflowEngineService result = airflowResolver.getWorkflowEngineService(workflowMetadata);

    // then
    assertThat(result).isNotNull();
    result.saveCustomOperator("operator", "operator.py");
    verify(externalWorkflowEngineService).saveCustomOperator("operator", "operator.py");
    verify(externalAirflowConfigService).getExternalAirflowConfig(SECRET_ID);
  }

  @Test
  void
      should_reuseExternalWorkflowEngineService_when_externalAirflowSecretIsResolvedAgain() {
    // given
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();

    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    IWorkflowEngineService refreshedExternalWorkflowEngineService =
        mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(
        EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService, refreshedExternalWorkflowEngineService);

    // when
    IWorkflowEngineService firstResult = airflowResolver.getWorkflowEngineService(workflowMetadata);
    IWorkflowEngineService result = airflowResolver.getWorkflowEngineService(workflowMetadata);

    // then
    assertThat(result).isSameAs(firstResult);
    verify(externalAirflowConfigService).getExternalAirflowConfig(SECRET_ID);
  }

  @Test
  void should_refreshExternalWorkflowEngineServiceAndRetry_when_airflowCallFails() {
    // given
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    IWorkflowEngineService externalWorkflowEngineService = mock(IWorkflowEngineService.class);
    IWorkflowEngineService refreshedExternalWorkflowEngineService =
        mock(IWorkflowEngineService.class);
    when(workflowEngineServiceFactory.createWorkflowEngineService(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineService, refreshedExternalWorkflowEngineService);
    doThrow(new AppException(500, "Error calling airflow", "Connection error"))
        .when(externalWorkflowEngineService)
        .saveCustomOperator("operator", "operator.py");

    // when
    IWorkflowEngineService result = airflowResolver.getWorkflowEngineService(workflowMetadata);
    result.saveCustomOperator("operator", "operator.py");

    // then
    verify(externalWorkflowEngineService).saveCustomOperator("operator", "operator.py");
    verify(refreshedExternalWorkflowEngineService).saveCustomOperator("operator", "operator.py");
    verify(externalAirflowConfigService, times(2)).getExternalAirflowConfig(SECRET_ID);
  }

  @Test
  void should_returnInternalWorkflowEngineExtension_when_noExternalAirflowSecretInMetadata() {
    // given
    WorkflowMetadata workflowMetadata =
        WorkflowMetadata.builder().registrationInstructions(Collections.emptyMap()).build();

    // when
    IWorkflowEngineExtension result = airflowResolver.getWorkflowEngineExtension(workflowMetadata);

    // then
    assertThat(result).isEqualTo(internalWorkflowEngineExtension);
  }

  @Test
  void should_returnInternalWorkflowEngineExtension_when_registrationInstructionsAreNull() {
    // given
    WorkflowMetadata workflowMetadata = WorkflowMetadata.builder().build();

    // when
    IWorkflowEngineExtension result = airflowResolver.getWorkflowEngineExtension(workflowMetadata);

    // then
    assertThat(result).isEqualTo(internalWorkflowEngineExtension);
  }

  @Test
  void
      should_returnExternalWorkflowEngineExtension_when_externalAirflowSecretIsInMetadataAndNotInCache() {
    // given
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();

    IWorkflowEngineExtension externalWorkflowEngineExtension = mock(IWorkflowEngineExtension.class);
    when(workflowEngineExtensionServiceFactory.createWorkflowEngineExtension(
        EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineExtension);

    // when
    IWorkflowEngineExtension result = airflowResolver.getWorkflowEngineExtension(workflowMetadata);

    // then
    assertThat(result).isNotNull();
    result.getLatestTaskDetails("dag", "run");
    verify(externalWorkflowEngineExtension).getLatestTaskDetails("dag", "run");
    verify(externalAirflowConfigService).getExternalAirflowConfig(SECRET_ID);
  }

  @Test
  void
      should_reuseExternalWorkflowEngineExtension_when_externalAirflowSecretIsResolvedAgain() {
    // given
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    IWorkflowEngineExtension externalWorkflowEngineExtension = mock(IWorkflowEngineExtension.class);
    IWorkflowEngineExtension refreshedExternalWorkflowEngineExtension =
        mock(IWorkflowEngineExtension.class);
    when(workflowEngineExtensionServiceFactory.createWorkflowEngineExtension(
        EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineExtension, refreshedExternalWorkflowEngineExtension);

    // when
    IWorkflowEngineExtension firstResult = airflowResolver.getWorkflowEngineExtension(workflowMetadata);
    IWorkflowEngineExtension result = airflowResolver.getWorkflowEngineExtension(workflowMetadata);

    // then
    assertThat(result).isSameAs(firstResult);
    verify(externalAirflowConfigService).getExternalAirflowConfig(SECRET_ID);
  }

  @Test
  void should_refreshExternalWorkflowEngineExtensionAndRetry_when_airflowCallFails() {
    // given
    WorkflowMetadata workflowMetadata =
        getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient();
    IWorkflowEngineExtension externalWorkflowEngineExtension = mock(IWorkflowEngineExtension.class);
    IWorkflowEngineExtension refreshedExternalWorkflowEngineExtension =
        mock(IWorkflowEngineExtension.class);
    Object latestTaskDetails = new Object();
    when(workflowEngineExtensionServiceFactory.createWorkflowEngineExtension(
            EXTERNAL_AIRFLOW_VERSION, airflowApiClient))
        .thenReturn(externalWorkflowEngineExtension, refreshedExternalWorkflowEngineExtension);
    when(externalWorkflowEngineExtension.getLatestTaskDetails("dag", "run"))
        .thenThrow(new AppException(500, "Error calling airflow", "Connection error"));
    when(refreshedExternalWorkflowEngineExtension.getLatestTaskDetails("dag", "run"))
        .thenReturn(latestTaskDetails);

    // when
    IWorkflowEngineExtension result = airflowResolver.getWorkflowEngineExtension(workflowMetadata);
    Object actualLatestTaskDetails = result.getLatestTaskDetails("dag", "run");

    // then
    assertThat(actualLatestTaskDetails).isSameAs(latestTaskDetails);
    verify(externalWorkflowEngineExtension).getLatestTaskDetails("dag", "run");
    verify(refreshedExternalWorkflowEngineExtension).getLatestTaskDetails("dag", "run");
    verify(externalAirflowConfigService, times(2)).getExternalAirflowConfig(SECRET_ID);
  }

  private WorkflowMetadata getWorkflowMetadataForExternalAirflowAndPrepareAirflowApiClient() {
    Map<String, Object> registrationInstructions = new HashMap<>();
    registrationInstructions.put(EXTERNAL_AIRFLOW_SECRET, SECRET_ID);
    WorkflowMetadata workflowMetadata =
        WorkflowMetadata.builder().registrationInstructions(registrationInstructions).build();

    ExternalAirflowConfig externalAirflowConfig =
        ExternalAirflowConfig.builder()
            .airflowVersion(EXTERNAL_AIRFLOW_VERSION)
            .airflowApiClientType(API_CLIENT_TYPE)
            .configMap(Collections.emptyMap())
            .build();
    when(externalAirflowConfigService.getExternalAirflowConfig(SECRET_ID))
        .thenReturn(externalAirflowConfig);

    AirflowConfig airflowConfig = new AirflowConfig();
    when(externalAirflowConfigFactory.createExternalAirflowConfig(
            API_CLIENT_TYPE, Collections.emptyMap()))
        .thenReturn(airflowConfig);

    when(airflowApiClientFactory.createAirflowApiClient(API_CLIENT_TYPE, airflowConfig))
        .thenReturn(airflowApiClient);

    return workflowMetadata;
  }
}
