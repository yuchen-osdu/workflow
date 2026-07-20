/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.osm.repository;

import com.google.api.client.http.HttpStatusCodes;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.legal.PersistenceException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.osm.core.model.Destination;
import org.opengroup.osdu.core.osm.core.model.Kind;
import org.opengroup.osdu.core.osm.core.model.Namespace;
import org.opengroup.osdu.core.osm.core.model.query.GetQuery;
import org.opengroup.osdu.core.osm.core.service.Context;
import org.opengroup.osdu.core.osm.core.translate.TranslatorException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.config.WorkflowPropertiesConfiguration;
import org.opengroup.osdu.workflow.osm.config.IDestinationProvider;
import org.opengroup.osdu.workflow.repository.ICommonMetadataRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.opengroup.osdu.core.osm.core.model.where.predicate.Eq.eq;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Repository
@Scope(SCOPE_SINGLETON)
@Slf4j
@Setter
@RequiredArgsConstructor
public class GcpOsmCommonMetadataRepository implements ICommonMetadataRepository {

  private static final String KEY_DAG_NAME = "dagName";
  public static final String WORKFLOW_NAME = "workflowName";
  private final WorkflowPropertiesConfiguration workflowConfig;
  private final IDestinationProvider destinationProvider;
  private final Context context;
  private final TenantInfo tenantInfo;
  private Class clazz = WorkflowMetadata.class;

  @Override
  public WorkflowMetadata createWorkflow(
      WorkflowMetadata workflowMetadata, boolean isSystemWorkflow) {

    Map<String, Object> instructions = workflowMetadata.getRegistrationInstructions();
    String workflowName = workflowMetadata.getWorkflowName();
    instructions.putIfAbsent(KEY_DAG_NAME, workflowName);

    if (Objects.isNull(workflowMetadata.getWorkflowId())
        || workflowMetadata.getWorkflowId().isEmpty()) {
      workflowMetadata.setWorkflowId(UUID.randomUUID().toString());
    }

    boolean workflowExist =
        getWorkflowMetadataByWorkflowName(workflowName, this.tenantInfo.getName(), isSystemWorkflow)
            .stream()
            .findFirst()
            .isPresent();

    if (workflowExist) {
      throw new AppException(
          HttpStatus.CONFLICT.value(),
          "Conflict",
          String.format("Workflow with name %s already exists.", workflowName));
    }
    return context.upsertAndGet(
        workflowMetadata, getDestination(this.tenantInfo.getName(), isSystemWorkflow));
  }

  @Override
  public WorkflowMetadata getWorkflow(String workflowName, boolean isSystemWorkflow) {
    log.info("Get details for workflow. Workflow name : {}", workflowName);
    List<WorkflowMetadata> results =
        getWorkflowMetadataByWorkflowName(
            workflowName, this.tenantInfo.getName(), isSystemWorkflow);
    log.debug("Count of found workflow meta data = {}", results.size());
    return results.stream()
        .findFirst()
        .orElseThrow(
            () ->
                new WorkflowNotFoundException(
                    String.format(
                        "Workflow entity for workflow name: %s not found.", workflowName)));
  }

  @Override
  public void deleteWorkflow(String workflowName, boolean isSystemWorkflow) {
    log.info("Delete workflow. Workflow name : {}", workflowName);
    try {
      context.delete(
          clazz,
          getDestination(this.tenantInfo.getName(), isSystemWorkflow),
          eq(WORKFLOW_NAME, workflowName));
    } catch (TranslatorException ex) {
      throw new PersistenceException(
          HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "Internal server error", ex.getMessage());
    }
  }

  @Override
  public List<WorkflowMetadata> getAllWorkflowForTenant(String prefix, boolean isSystemWorkflow) {
    log.info("Get all workflows. Prefix {}", prefix);
    GetQuery<WorkflowMetadata> getQuery =
        new GetQuery<>(clazz, getDestination(this.tenantInfo.getName(), isSystemWorkflow));
    List<WorkflowMetadata> workflowMetadataList = context.getResultsAsList(getQuery);

    return workflowMetadataList.stream()
        .filter(c -> Objects.isNull(prefix) || c.getWorkflowName().startsWith(prefix))
        .collect(Collectors.toList());
  }

  private List<WorkflowMetadata> getWorkflowMetadataByWorkflowName(
      String workflowName, String tenant, boolean isSystemWorkflow) {
    GetQuery<WorkflowMetadata> workflowMetadata =
        new GetQuery<>(
            clazz, getDestination(tenant, isSystemWorkflow), eq(WORKFLOW_NAME, workflowName));
    return context.getResultsAsList(workflowMetadata);
  }

  private Destination getDestinationForSystemWorkflow(
      String tenantName, String namespace, String kind) {
    Kind kindEntity = new Kind(kind);
    return Destination.builder()
        .partitionId(tenantName)
        .namespace(new Namespace(namespace))
        .kind(kindEntity)
        .build();
  }

  private Destination getDestination(String tenantName, boolean isSystemWorkflow) {
    if (isSystemWorkflow) {
      return getDestinationForSystemWorkflow(
          tenantName,
          workflowConfig.getSystemWorkflowNamespace(),
          workflowConfig.getSystemWorkflowKind());
    } else {
      return destinationProvider.getDestination(tenantName, workflowConfig.getWorkflowKind());
    }
  }
}
