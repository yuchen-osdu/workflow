/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.gcp.workflow.workflow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.gcp.workflow.util.HTTPClientGCP;
import org.opengroup.osdu.workflow.workflow.v3.PostCreateSystemWorkflowV3IntegrationTests;

import java.util.ArrayList;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

@Slf4j
public class TestPostCreateSystemWorkflowV3Integration
    extends PostCreateSystemWorkflowV3IntegrationTests {
  @BeforeEach
  @Override
  public void setup() {
    this.client = new HTTPClientGCP();
    this.headers = client.getCommonHeaderWithoutPartition();
    try {
      deleteTestSystemWorkflows(CREATE_WORKFLOW_WORKFLOW_NAME);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @AfterEach
  @Override
  public void tearDown() {
    deleteAllTestWorkflowRecords();
    this.client = null;
    this.headers = null;
    this.createdWorkflows = new ArrayList<>();
  }

  private void deleteAllTestWorkflowRecords() {
    createdWorkflows.stream()
        .forEach(
            c -> {
              try {
                deleteTestSystemWorkflows(c.get(WORKFLOW_NAME_FIELD));
              } catch (Exception e) {
                log.error(e.getMessage(), e);
              }
            });
  }
}
