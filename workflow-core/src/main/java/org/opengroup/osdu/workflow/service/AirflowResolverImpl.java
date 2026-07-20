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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.info.ConnectedOuterService;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ExternalAirflowConfig;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IExternalAirflowConfigService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.factory.AirflowApiClientFactory;
import org.opengroup.osdu.workflow.service.factory.ExternalAirflowConfigFactory;
import org.opengroup.osdu.workflow.service.factory.WorkflowEngineExtensionServiceFactory;
import org.opengroup.osdu.workflow.service.factory.WorkflowEngineServiceFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirflowResolverImpl implements IAirflowResolver {

  private static final String EXTERNAL_AIRFLOW_SECRET = "externalAirflowSecret";
  private static final String N_A = "N/A";
  private static final String INTERNAL_AIRFLOW = "Internal Airflow";
  private static final String EXTERNAL_AIRFLOW = "External Airflow: ";

  private final IWorkflowEngineService internalWorkflowEngineService;
  private final IWorkflowEngineExtension internalWorkflowEngineExtension;
  private final Map<String, IWorkflowEngineService> externalAirflowMap = new ConcurrentHashMap<>();
  private final Map<String, IWorkflowEngineExtension> externalAirflowExtensionMap = new ConcurrentHashMap<>();

  private final WorkflowEngineServiceFactory workflowEngineServiceFactory;
  private final WorkflowEngineExtensionServiceFactory workflowEngineExtensionServiceFactory;
  private final AirflowApiClientFactory airflowApiClientFactory;
  private final ExternalAirflowConfigFactory externalAirflowConfigFactory;
  private final IExternalAirflowConfigService externalAirflowConfigService;

  @Override
  public List<ConnectedOuterService> getConnectedWorkflowEngineServicesVersions() {
    List<ConnectedOuterService> connectedOuterServices = new ArrayList<>();
    connectedOuterServices.add(
        ConnectedOuterService.builder()
            .name(INTERNAL_AIRFLOW)
            .version(internalWorkflowEngineService.getVersion().orElse(N_A))
            .build());
    externalAirflowMap.entrySet().stream()
        .map(
            e ->
                ConnectedOuterService.builder()
                    .name(EXTERNAL_AIRFLOW + e.getKey())
                    .version(e.getValue().getVersion().orElse(N_A))
                    .build())
        .forEach(connectedOuterServices::add);
    return connectedOuterServices;
  }

  @Override
  public IWorkflowEngineService getWorkflowEngineService(WorkflowMetadata workflowMetadata) {
    log.debug(
        "Getting workflow engine service for workflow: {}", workflowMetadata.getWorkflowName());
    Optional<String> externalAirflowSecretId = getExternalAirflowSecretId(workflowMetadata);
    if (externalAirflowSecretId.isPresent()) {
      String secretId = externalAirflowSecretId.get();
      log.debug("Using external airflow engine for secret id: {}", secretId);
      return externalAirflowMap.computeIfAbsent(
          secretId,
          s -> {
            AirflowVersionAndAirflowApiClient airflowVersionAndAirflowApiClient =
                getAirflowVersionAndApiClientFromSecret(s);

            return workflowEngineServiceFactory.createWorkflowEngineService(
                airflowVersionAndAirflowApiClient.airflowVersion(),
                airflowVersionAndAirflowApiClient.airflowApiClient());
          });
    }
    log.debug("Using internal airflow engine");
    return internalWorkflowEngineService;
  }

  @Override
  public IWorkflowEngineExtension getWorkflowEngineExtension(WorkflowMetadata workflowMetadata) {
    log.debug(
        "Getting workflow engine extension for workflow: {}", workflowMetadata.getWorkflowName());
    Optional<String> externalAirflowSecretId = getExternalAirflowSecretId(workflowMetadata);
    if (externalAirflowSecretId.isPresent()) {
      String secretId = externalAirflowSecretId.get();
      log.debug("Using external airflow engine extension for secret id: {}", secretId);
      return externalAirflowExtensionMap.computeIfAbsent(
          secretId,
          s -> {
            AirflowVersionAndAirflowApiClient airflowVersionAndAirflowApiClient =
                getAirflowVersionAndApiClientFromSecret(s);

            return workflowEngineExtensionServiceFactory.createWorkflowEngineExtension(
                airflowVersionAndAirflowApiClient.airflowVersion(),
                airflowVersionAndAirflowApiClient.airflowApiClient());
          });
    }
    log.debug("Using internal airflow engine extension");
    return internalWorkflowEngineExtension;
  }

  private Optional<String> getExternalAirflowSecretId(WorkflowMetadata workflowMetadata) {
    log.debug(
        "Getting external airflow secret id for workflow: {}", workflowMetadata.getWorkflowName());
    if (workflowMetadata.getRegistrationInstructions() != null
        && workflowMetadata.getRegistrationInstructions().get(EXTERNAL_AIRFLOW_SECRET) != null) {
      String secretId =
          (String) workflowMetadata.getRegistrationInstructions().get(EXTERNAL_AIRFLOW_SECRET);
      log.debug("Found external airflow secret id: {}", secretId);
      return Optional.of(secretId);
    }
    log.debug("No external airflow secret id found");
    return Optional.empty();
  }

  private AirflowVersionAndAirflowApiClient getAirflowVersionAndApiClientFromSecret(
      String secretId) {
    log.info("Getting airflow version and api client from secret: {}", secretId);
    // Retrieve the ExternalAirflowConfig from the secret ID
    ExternalAirflowConfig externalAirflowConfig =
        externalAirflowConfigService.getExternalAirflowConfig(secretId);

    // Create AirflowConfig from the ExternalAirflowConfig
    AirflowConfig airflowConfig =
        externalAirflowConfigFactory.createExternalAirflowConfig(
            externalAirflowConfig.getAirflowApiClientType(), externalAirflowConfig.getConfigMap());

    // Create the AirflowApiClient using the AirflowConfig
    IAirflowApiClient airflowApiClient =
        airflowApiClientFactory.createAirflowApiClient(
            externalAirflowConfig.getAirflowApiClientType(), airflowConfig);

    return new AirflowVersionAndAirflowApiClient(
        externalAirflowConfig.getAirflowVersion(), airflowApiClient);
  }

  private record AirflowVersionAndAirflowApiClient(
      String airflowVersion, IAirflowApiClient airflowApiClient) {}
}
