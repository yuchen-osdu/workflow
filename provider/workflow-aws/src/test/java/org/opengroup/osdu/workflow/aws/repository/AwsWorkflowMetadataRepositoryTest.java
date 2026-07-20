/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.workflow.aws.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.WorkflowMetadataDoc;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;

import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;

@ExtendWith(MockitoExtension.class)
class AwsWorkflowMetadataRepositoryTest {

    private static final String WORKFLOW_NAME = "workflowName";
    private static final String PREFIX = "prefix";
    private static final String TABLE_PARAM_PATH = "test/path";

    private AwsWorkflowMetadataRepository repository;

    @Mock
    private IDynamoDBQueryHelperFactory queryHelperFactory;

    @Mock
    private DynamoDBQueryHelper<WorkflowMetadataDoc> queryHelper;

    @Mock
    private DpsHeaders headers;

    @BeforeEach
    void setup() {

        when(queryHelperFactory.createQueryHelper(headers, TABLE_PARAM_PATH, WorkflowMetadataDoc.class)).thenReturn(queryHelper);

        repository = new AwsWorkflowMetadataRepository(queryHelperFactory, TABLE_PARAM_PATH, headers);
    }

    @Test
    void testCreateWorkflow_Success() {
        //Arrange
        WorkflowMetadata workflowMetadata = new WorkflowMetadata();
        workflowMetadata.setWorkflowName(WORKFLOW_NAME);

        // Act
        repository.createWorkflow(workflowMetadata);

        // Assert
        verify(queryHelper, times(1)).putItem(any(PutItemEnhancedRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateWorkflow_ConditionalCheckFailedException() {
        // Arrange
        WorkflowMetadata workflowMetadata = new WorkflowMetadata();
        workflowMetadata.setWorkflowName(WORKFLOW_NAME);

        // Act - should throw ResourceConflictException
        doThrow(ConditionalCheckFailedException.class).when(queryHelper).putItem(any(PutItemEnhancedRequest.class));

        // Assert
        Assertions.assertThrows(ResourceConflictException.class, () -> {
            repository.createWorkflow(workflowMetadata);
        });
    }


    @SuppressWarnings("unchecked")
    @Test
    void testCreateWorkflow_DynamoDbException() {
        // Arrange
        WorkflowMetadata workflowMetadata = new WorkflowMetadata();
        workflowMetadata.setWorkflowName(WORKFLOW_NAME);

        // Act - should throw ResourceConflictException
        doThrow(DynamoDbException.class).when(queryHelper).putItem(any(PutItemEnhancedRequest.class));

        // Assert
        Assertions.assertThrows(AppException.class, () -> {
            repository.createWorkflow(workflowMetadata);
        });
    }

    @Test
    void testGetWorkflow() {
        // Arrange
        WorkflowMetadataDoc doc = mock(WorkflowMetadataDoc.class);
        WorkflowMetadata metadata = new WorkflowMetadata();
        when(doc.convertToWorkflowMetadata()).thenReturn(metadata);

        // Use anyString() matchers for both parameters
        when(queryHelper.getItem(any(), any())).thenReturn(Optional.of(doc));

        // Act
        WorkflowMetadata result = repository.getWorkflow(WORKFLOW_NAME);

        // Assert
        Assertions.assertEquals(metadata, result);
        verify(doc, times(1)).convertToWorkflowMetadata();
    }

    @Test
    void testGetWorkflow_NotFound() {
        // Arrange
        when(queryHelper.getItem(any(), any())).thenReturn(Optional.empty());

        // Act & Assert - should throw WorkflowNotFoundException
        Assertions.assertThrows(WorkflowNotFoundException.class, () -> {
            repository.getWorkflow(WORKFLOW_NAME);
        });
    }

    @Test
    void testGetWorkflow_InternalServerErrorException() {
        // Arrange
        doThrow(InternalServerErrorException.class).when(queryHelper).getItem(any(), any());

        // Act & Assert - should throw WorkflowNotFoundException
        Assertions.assertThrows(AppException.class, () -> {
            repository.getWorkflow(WORKFLOW_NAME);
        });
    }

    @Test
    void testGetWorkflow_DynamoDbException() {
        // Arrange
        doThrow(DynamoDbException.class).when(queryHelper).getItem(any(), any());

        // Act & Assert - should throw WorkflowNotFoundException
        Assertions.assertThrows(AppException.class, () -> {
            repository.getWorkflow(WORKFLOW_NAME);
        });
    }

    @Test
    void testDeleteWorkflow() {
        // Arrange
        doNothing().when(queryHelper).deleteItem(any(), any());

        // Act
        repository.deleteWorkflow(WORKFLOW_NAME);

        // Assert
        verify(queryHelper, times(1)).deleteItem(any(), any());
    }

    @Test
    void testGetAllWorkflowForTenant() {
        // Arrange
        WorkflowMetadataDoc doc = mock(WorkflowMetadataDoc.class);
        WorkflowMetadata metadata = new WorkflowMetadata();
        when(doc.convertToWorkflowMetadata()).thenReturn(metadata);

        QueryPageResult<WorkflowMetadataDoc> pageResult = new QueryPageResult<>(
                Collections.singletonList(doc), null, null);
        when(queryHelper.scanPage(any(ScanEnhancedRequest.class))).thenReturn(pageResult);

        // Act
        List<WorkflowMetadata> result = repository.getAllWorkflowForTenant(PREFIX);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(metadata, result.get(0));
    }

    @Test
    void testGetAllWorkflowForTenant_EmptyResult() {
        // Arrange
        QueryPageResult<WorkflowMetadataDoc> pageResult = new QueryPageResult<>(
                Collections.emptyList(), null, null);
        when(queryHelper.scanPage(any(ScanEnhancedRequest.class))).thenReturn(pageResult);

        // Act
        List<WorkflowMetadata> result = repository.getAllWorkflowForTenant(PREFIX);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllWorkflowForTenant_DynamoDbException() {
        // Arrange
        doThrow(DynamoDbException.class).when(queryHelper).scanPage(any(ScanEnhancedRequest.class));

        // Act & Assert
        Assertions.assertThrows(AppException.class, () -> {
            repository.getAllWorkflowForTenant(PREFIX);
        });
    }
}
