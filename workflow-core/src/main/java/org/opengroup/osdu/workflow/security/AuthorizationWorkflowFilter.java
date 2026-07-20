/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.security;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationWorkflowFilter {

  private final DpsHeaders dpsHeaders;
  private final IWorkflowRunRepository workflowRunRepository;

  public boolean isCreatorOf(String workflowName, String runId) {
    WorkflowRun workflowRun = workflowRunRepository.getWorkflowRun(workflowName, runId);
    return workflowRun.getSubmittedBy().equals(dpsHeaders.getUserEmail());
  }
}
