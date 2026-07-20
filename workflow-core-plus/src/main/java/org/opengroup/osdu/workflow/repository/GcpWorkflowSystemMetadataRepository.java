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
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowSystemMetadataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GcpWorkflowSystemMetadataRepository implements IWorkflowSystemMetadataRepository {
  private final ICommonMetadataRepository commonMetadataRepository;

  /**
   * Returns workflow metadata based on workflowName
   *
   * @param workflowName Name of the workflow for which metadata should be retrieved.
   * @return Workflow metadata
   */
  @Override
  public WorkflowMetadata getSystemWorkflow(String workflowName) {
    return commonMetadataRepository.getWorkflow(workflowName, true);
  }

  /**
   * Creates workflow metadata record in persistence store.
   *
   * @param workflowMetadata Workflow metadata object to save in persistence store.
   * @return Workflow metadata
   */
  @Override
  public WorkflowMetadata createSystemWorkflow(WorkflowMetadata workflowMetadata) {
    return commonMetadataRepository.createWorkflow(workflowMetadata, true);
  }

  /**
   * Deletes workflow metadata based on workflowName
   *
   * @param workflowName Name of the workflow for which metadata should be deleted.
   */
  @Override
  public void deleteSystemWorkflow(String workflowName) {
    commonMetadataRepository.deleteWorkflow(workflowName, true);
  }

  /**
   * Get all system workflows metadata based on prefix
   *
   * @param prefix Name of the system workflow for which metadata should be deleted.
   */
  @Override
  public List<WorkflowMetadata> getAllSystemWorkflow(String prefix) {
   return commonMetadataRepository.getAllWorkflowForTenant(prefix, true);
  }
}
