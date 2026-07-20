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

package org.opengroup.osdu.workflow.provider.azure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.azure.storage.blob.sas.BlobContainerSasPermission;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.blobstorage.BlobStore;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.provider.azure.api.CustomOperatorApi;
import org.opengroup.osdu.workflow.provider.azure.config.CosmosConfig;
import org.opengroup.osdu.workflow.provider.azure.model.WorkflowTasksSharingDoc;

@ExtendWith(MockitoExtension.class)
public class WorkflowTasksSharingRepositoryTest {

  private static final String TEST_WORKFLOW_NAME = "test-workflow-name";
  private static final String TEST_RUN_ID = "test-run-id";
  private static final String DATABASE_NAME = "database";
  private static final String PARTITION_ID = "partition-id";
  private static final String WORKFLOW_TASKS_SHARING_COLLECTION_NAME = "workflow-tasks-sharing-collection";
  private static final String CONTAINER_ID = "container-id";

  @Mock
  BlobStore blobStore;

  @Mock
  private CosmosStore cosmosStore;

  @Mock
  private CosmosConfig cosmosConfig;

  @Mock
  private DpsHeaders dpsHeaders;

  @Mock
  private JaxRsDpsLog logger;

  @Mock
  private CustomOperatorApi customOperatorApi;

  @InjectMocks
  private WorkflowTasksSharingRepository sut;

  @BeforeEach
  public void init() {
    doReturn(DATABASE_NAME).when(cosmosConfig).getDatabase();
    doReturn(WORKFLOW_TASKS_SHARING_COLLECTION_NAME).when(cosmosConfig).getWorkflowTasksSharingCollection();
  }

  @Test
  public void testGetSignedUrl_whenContainerExists_thenReturnsSignedUrlForExistingContainer() {
    WorkflowTasksSharingDoc workflowTasksSharingDoc = mock(WorkflowTasksSharingDoc.class);
    doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
    doReturn(CONTAINER_ID).when(workflowTasksSharingDoc).getContainerId();
    doReturn(Optional.of(workflowTasksSharingDoc)).when(cosmosStore).findItem(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_TASKS_SHARING_COLLECTION_NAME),
        eq(TEST_RUN_ID),
        eq(TEST_WORKFLOW_NAME),
        eq(WorkflowTasksSharingDoc.class));

    sut.getSignedUrl(TEST_WORKFLOW_NAME, TEST_RUN_ID);

    ArgumentCaptor<String> containerIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<BlobContainerSasPermission> blobContainerSasPermissionArgumentCaptor = ArgumentCaptor.forClass(BlobContainerSasPermission.class);

    verify(blobStore).generatePreSignedUrlWithUserDelegationSas(eq(PARTITION_ID), containerIdCaptor.capture(), any(), any(), blobContainerSasPermissionArgumentCaptor.capture());

    String containerId = containerIdCaptor.getValue();
    BlobContainerSasPermission blobContainerSasPermission = blobContainerSasPermissionArgumentCaptor.getValue();

    assertEquals(containerId, workflowTasksSharingDoc.getContainerId());
    checkBlobContainerSasPermission(blobContainerSasPermission);
  }

  @Test
  public void testGetSignedUrl_whenContainerDoesNotExist_thenCreateContainerAndReturnSignedUrl() {
    doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
    sut.getSignedUrl(TEST_WORKFLOW_NAME, TEST_RUN_ID);

    ArgumentCaptor<String> containerIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<WorkflowTasksSharingDoc> workflowTasksSharingDocArgumentCaptor = ArgumentCaptor.forClass(WorkflowTasksSharingDoc.class);
    ArgumentCaptor<BlobContainerSasPermission> blobContainerSasPermissionArgumentCaptor = ArgumentCaptor.forClass(BlobContainerSasPermission.class);

    verify(blobStore).createBlobContainer(eq(PARTITION_ID), containerIdCaptor.capture());
    String containerId = containerIdCaptor.getValue();
    verify(blobStore).generatePreSignedUrlWithUserDelegationSas(eq(PARTITION_ID), eq(containerId), any(), any(), blobContainerSasPermissionArgumentCaptor.capture());
    BlobContainerSasPermission blobContainerSasPermission = blobContainerSasPermissionArgumentCaptor.getValue();
    verify(cosmosStore).createItem(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_TASKS_SHARING_COLLECTION_NAME),
        eq(TEST_WORKFLOW_NAME),
        workflowTasksSharingDocArgumentCaptor.capture()
    );
    WorkflowTasksSharingDoc workflowTasksSharingDoc = workflowTasksSharingDocArgumentCaptor.getValue();
    assertEquals(workflowTasksSharingDoc.getContainerId(), containerId);
    assertEquals(workflowTasksSharingDoc.getWorkflowName(), TEST_WORKFLOW_NAME);
    assertEquals(workflowTasksSharingDoc.getRunId(), TEST_RUN_ID);
    assertEquals(workflowTasksSharingDoc.getPartitionKey(), TEST_WORKFLOW_NAME);
    assertEquals(workflowTasksSharingDoc.getId(), TEST_RUN_ID);
    checkBlobContainerSasPermission(blobContainerSasPermission);
  }

  @Test
  public void testDeleteTasksSharingInfoContainer() {
    WorkflowTasksSharingDoc workflowTasksSharingDoc = mock(WorkflowTasksSharingDoc.class);

    doReturn(CONTAINER_ID).when(workflowTasksSharingDoc).getContainerId();
    doReturn(Optional.of(workflowTasksSharingDoc)).when(cosmosStore).findItem(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_TASKS_SHARING_COLLECTION_NAME),
        eq(TEST_RUN_ID),
        eq(TEST_WORKFLOW_NAME),
        eq(WorkflowTasksSharingDoc.class));

    sut.deleteTasksSharingInfoContainer(PARTITION_ID, TEST_WORKFLOW_NAME, TEST_RUN_ID);

    verify(blobStore, times(1)).deleteBlobContainer(eq(PARTITION_ID), eq(CONTAINER_ID));
    verify(cosmosStore, times(1)).deleteItem(
        eq(PARTITION_ID),
        eq(DATABASE_NAME),
        eq(WORKFLOW_TASKS_SHARING_COLLECTION_NAME),
        eq(TEST_RUN_ID),
        eq(TEST_WORKFLOW_NAME)
    );
  }

  private void checkBlobContainerSasPermission(BlobContainerSasPermission blobContainerSasPermission) {
    assertEquals(blobContainerSasPermission.hasAddPermission(), false);
    assertEquals(blobContainerSasPermission.hasCreatePermission(), true);
    assertEquals(blobContainerSasPermission.hasDeletePermission(), false);
    assertEquals(blobContainerSasPermission.hasDeleteVersionPermission(), false);
    assertEquals(blobContainerSasPermission.hasListPermission(), true);
    assertEquals(blobContainerSasPermission.hasReadPermission(), true);
    assertEquals(blobContainerSasPermission.hasTagsPermission(), false);
    assertEquals(blobContainerSasPermission.hasWritePermission(), true);
  }
}
