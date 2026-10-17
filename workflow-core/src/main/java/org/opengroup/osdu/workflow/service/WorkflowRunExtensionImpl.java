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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.exception.WorkflowRunNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowResolver;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunExtension;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowRunExtensionImpl implements IWorkflowRunExtension {
  protected static final String DAG_NAME = "dagName";
  protected static final String EXTERNAL_AIRFLOW_SECRET = "externalAirflowSecret";

  private final IWorkflowManagerService managerService;
  private final IAirflowResolver airflowResolver;
  private final IWorkflowRunRepository workflowRunRepository;
  private final InternalAirflowExtensions internalAirflowExtensions;

  @Override
  public Object getLatestTaskDetails(String workflowName, String runId) {
    WorkflowMetadata workflowMetadata = managerService.getWorkflowByName(workflowName);
    Map<String, Object> instructions = workflowMetadata.getRegistrationInstructions();
    String dagName =
        instructions != null && instructions.get(DAG_NAME) != null
            ? (String) instructions.get(DAG_NAME)
            : workflowMetadata.getWorkflowName();
    return resolveExtension(workflowMetadata, workflowName, runId).getLatestTaskDetails(dagName, runId);
  }

  /**
   * External-Airflow workflows keep using the secret-resolved engine. For internal runs the
   * extension is chosen by the run's persisted {@code engineVersion} so {@code /latestInfo} hits the
   * engine that owns the run (in-flight Airflow 2 runs stay on Airflow 2 during migration).
   */
  private IWorkflowEngineExtension resolveExtension(
      WorkflowMetadata workflowMetadata, String workflowName, String runId) {
    Map<String, Object> instructions = workflowMetadata.getRegistrationInstructions();
    if (instructions != null && instructions.get(EXTERNAL_AIRFLOW_SECRET) != null) {
      return airflowResolver.getWorkflowEngineExtension(workflowMetadata);
    }
    // Airflow 2-only deployment (every provider that never enables Airflow 3): there is no engine
    // to route to other than the default, so skip the per-run DB read entirely. This keeps
    // /latestInfo behavior byte-for-byte identical to before for all AF2-only providers and avoids
    // a spurious 404 for runs that exist in Airflow but not the workflow DB.
    if (!internalAirflowExtensions.hasVersionedRouting()) {
      return internalAirflowExtensions.forEngineVersion(null);
    }
    return internalAirflowExtensions.forEngineVersion(lookupEngineVersion(workflowName, runId));
  }

  /**
   * Best-effort read of the run's persisted engine version. Some runs exist in Airflow but not in
   * the workflow DB (natively-triggered/system DAGs, TTL'd records); for those the repository throws
   * a not-found exception. Rather than failing {@code /latestInfo} with a 404 (a behavior change for
   * all providers), fall back to {@code null} so the run resolves on Airflow 2 and Airflow decides —
   * preserving the pre-existing behavior. Providers surface not-found differently, so both the typed
   * {@link WorkflowNotFoundException}/{@link WorkflowRunNotFoundException} and a bare
   * {@link AppException} with HTTP 404 are treated as "not in DB".
   */
  private String lookupEngineVersion(String workflowName, String runId) {
    try {
      return workflowRunRepository.getWorkflowRun(workflowName, runId).getEngineVersion();
    } catch (WorkflowNotFoundException | WorkflowRunNotFoundException e) {
      logRunNotFound(workflowName, runId);
      return null;
    } catch (AppException e) {
      if (e.getError() != null && e.getError().getCode() == 404) {
        logRunNotFound(workflowName, runId);
        return null;
      }
      throw e;
    }
  }

  private void logRunNotFound(String workflowName, String runId) {
    log.debug(
        "Workflow run {} for {} not found in DB; resolving /latestInfo on the default engine.",
        runId,
        workflowName);
  }
}
