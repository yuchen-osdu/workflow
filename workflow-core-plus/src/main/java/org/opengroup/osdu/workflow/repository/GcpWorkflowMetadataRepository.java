/*
  Copyright 2021 Google LLC
  Copyright 2021 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package org.opengroup.osdu.workflow.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GcpWorkflowMetadataRepository implements IWorkflowMetadataRepository {
  private final ICommonMetadataRepository commonMetadataRepository;

  @Override
  public WorkflowMetadata createWorkflow(WorkflowMetadata workflowMetadata) {
    return commonMetadataRepository.createWorkflow(workflowMetadata, false);
  }

  @Override
  public WorkflowMetadata getWorkflow(String workflowName) {
    return commonMetadataRepository.getWorkflow(workflowName, false);
  }

  @Override
  public void deleteWorkflow(String workflowName) {
    commonMetadataRepository.deleteWorkflow(workflowName, false);
  }

  @Override
  public List<WorkflowMetadata> getAllWorkflowForTenant(final String prefix) {
    return commonMetadataRepository.getAllWorkflowForTenant(prefix, false);
  }
}
