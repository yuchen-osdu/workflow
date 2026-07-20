/*
 *  Copyright 2020-2021 Google LLC
 *  Copyright 2020-2021 EPAM Systems, Inc
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

import static org.opengroup.osdu.core.osm.core.model.where.condition.And.and;
import static org.opengroup.osdu.core.osm.core.model.where.predicate.Eq.eq;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import com.google.api.client.http.HttpStatusCodes;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.legal.PersistenceException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.osm.core.model.query.GetQuery;
import org.opengroup.osdu.core.osm.core.service.Context;
import org.opengroup.osdu.core.osm.core.service.Results;
import org.opengroup.osdu.core.osm.core.translate.TranslatorException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import org.opengroup.osdu.workflow.config.WorkflowPropertiesConfiguration;
import org.opengroup.osdu.workflow.osm.config.IDestinationProvider;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Repository
@Scope(SCOPE_SINGLETON)
@Slf4j
@RequiredArgsConstructor
public class GcpOsmWorkflowRunRepository implements IWorkflowRunRepository {

  private static final String INCORRECT_RUN_ID_PREFIX = "backfill";
  public static final String WORKFLOW_NAME = "workflowName";
  public static final String RUN_ID = "runId";
  public static final String LIMIT_PARAM = "limit";
  public static final String CURSOR_PARAM = "cursor";
  public static final String PREFIX_PARAM = "prefix";
  public static final String START_DATE_PARAM = "startDate";
  public static final String END_DATE_PARAM = "endDate";
  private final WorkflowPropertiesConfiguration workflowConfig;
  private final IDestinationProvider destinationProvider;
  private final Context context;
  private final TenantInfo tenantInfo;

  @Override
  public WorkflowRun saveWorkflowRun(WorkflowRun workflowRun) {
    log.info("Saving workflow run. Workflow name : {}", workflowRun.getWorkflowName());
    return context.upsertAndGet(workflowRun,
        this.destinationProvider.getDestination(this.tenantInfo,
            workflowConfig.getWorkflowRunKind()));
  }

  @Override
  public WorkflowRun getWorkflowRun(String workflowName, String runId) {
    log.info(
        "Get execution instances for workflow. Workflow name : {}, run Id : {}", workflowName,
        runId);
    GetQuery<WorkflowRun>.GetQueryBuilder<WorkflowRun> queryBuilder =
        new GetQuery<>(WorkflowRun.class, this.destinationProvider.getDestination(this.tenantInfo,
            workflowConfig.getWorkflowRunKind())).toBuilder();
    queryBuilder.where(and(eq(WORKFLOW_NAME, workflowName), eq(RUN_ID, runId))).build();
    return context.getResultsAsList(queryBuilder.build()).stream().findFirst()
        .orElseThrow(() -> new WorkflowNotFoundException(
            String.format("Workflow entity for workflow name: %s and run id: %s not found.",
                workflowName,
                runId)));
  }

  @Override
  public WorkflowRunsPage getWorkflowRunsByWorkflowName(String workflowName, Integer limit,
      String cursor) {
    GetQuery<WorkflowRun> getQuery =
        new GetQuery<>(WorkflowRun.class, this.destinationProvider.getDestination(this.tenantInfo,
            workflowConfig.getWorkflowRunKind()), eq(WORKFLOW_NAME, workflowName));
    Results<WorkflowRun, WorkflowRun> results = context.getResults(getQuery, null, limit, cursor);
    if (limit != 0 && results.outcome().getList().size() < limit) {
      return new WorkflowRunsPage(results.outcome().getList(), null);
    }
    return new WorkflowRunsPage(results.outcome().getList(), results.outcome().getPointer());
  }

  @Override
  public void deleteWorkflowRuns(String workflowName, List<String> runIds) {
    log.info("Delete workflow run with id's. Workflow name : {}", workflowName);
    for (String runId : runIds) {
      try {
        context.delete(WorkflowRun.class, this.destinationProvider.getDestination(this.tenantInfo,
                workflowConfig.getWorkflowRunKind()),
            and(eq(WORKFLOW_NAME, workflowName), eq(RUN_ID, runId)));
      } catch (TranslatorException ex) {
        throw new PersistenceException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR,
            "Internal server error", ex.getMessage());
      }
    }
  }

  @Override
  public WorkflowRun updateWorkflowRun(WorkflowRun workflowRun) {
    return context.upsertAndGet(workflowRun,
        this.destinationProvider.getDestination(this.tenantInfo,
            workflowConfig.getWorkflowRunKind()));
  }

  @Override
  public List<WorkflowRun> getAllRunInstancesOfWorkflow(String workflowName,
      Map<String, Object> params) {
    log.info("Get all run instances of workflow. Workflow name : {}", workflowName);
    Integer limit = null;
    if (params.get(LIMIT_PARAM) != null) {
      try {
        limit = Integer.parseInt((String) params.get(LIMIT_PARAM));
      } catch (NumberFormatException e) {
        throw new AppException(
            HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
            "Not valid limit param format.",
            e.getMessage()
        );
      }
    }
    String cursor = (String) params.get(CURSOR_PARAM);
    GetQuery<WorkflowRun> getQuery =
        new GetQuery<>(
            WorkflowRun.class,
            this.destinationProvider.getDestination(this.tenantInfo, workflowConfig.getWorkflowRunKind()),
            eq(WORKFLOW_NAME, workflowName)
        );

    List<WorkflowRun> responseList =
        context.getResults(getQuery, null, limit, cursor).outcome().getList();
    if (!params.isEmpty()) {
      return filterWorkflowEntities(responseList, params);
    }
    return responseList;
  }

  private List<WorkflowRun> filterWorkflowEntities(List<WorkflowRun> workflowRunList,
      Map<String, Object> params) {
    List<WorkflowRun> resultList;
    String prefix = (String) params.get(PREFIX_PARAM);
    String startDate = (String) params.get(START_DATE_PARAM);
    String endDate = (String) params.get(END_DATE_PARAM);

    resultList = workflowRunList.stream().filter(c -> {
      if (INCORRECT_RUN_ID_PREFIX.equals(prefix) || Objects.nonNull(prefix) &&
          !c.getRunId().startsWith(prefix)) {
        return false;
      }
      if (Objects.nonNull(startDate) && c.getStartTimeStamp() <= Long.parseLong(startDate)) {
        return false;
      }
      return !Objects.nonNull(endDate) || c.getEndTimeStamp() < Long.parseLong(endDate);
    }).collect(Collectors.toList());
    return resultList;
  }
}
