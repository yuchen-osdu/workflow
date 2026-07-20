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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.GsiQueryRequest;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.aws.config.AwsServiceConfig;
import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.WorkflowRunDoc;
import org.opengroup.osdu.workflow.exception.WorkflowRunNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowRun;


import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AwsWorkflowRunRepositoryTest {

    private final String PARTITION = "data-partition-id";
    private final String WORKFLOWNAME = "workflowName";
    private final String RUNID = "runId";
    private final String CURSOR = "{\"dummyKey\":{\"S\":\"dummyValue\"}}";;
    private final String TABLEPARAMETERPATH = "workflowRunTablePath";

    private AwsWorkflowRunRepository repo;

    @Mock
    private DpsHeaders headers;

    @Mock
    private IDynamoDBQueryHelperFactory queryHelperFactory;

    @Mock
    private DynamoDBQueryHelper<WorkflowRunDoc> queryHelper;

    @Mock
    private AwsServiceConfig awsServiceConfig;

    @BeforeEach
    void setup() {
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(PARTITION);
        when(queryHelperFactory.createQueryHelper(
                headers, TABLEPARAMETERPATH, WorkflowRunDoc.class))
                .thenReturn(queryHelper);
        
        repo = new AwsWorkflowRunRepository(queryHelperFactory, TABLEPARAMETERPATH, headers, awsServiceConfig);
    }



    @Test
    void testSaveWorkflowRun_success() {
        WorkflowRun workflowRun = mock(WorkflowRun.class);
        
        repo.saveWorkflowRun(workflowRun);

        verify(queryHelper, times(1)).putItem(Mockito.any(PutItemEnhancedRequest.class));
    }


    @Test
    void testSaveWorkflowRunConditionalCheckFailedException() {
        WorkflowRun workflowRun = mock(WorkflowRun.class);

        // Act - should throw ResourceConflictException
        doThrow(ConditionalCheckFailedException.class).when(queryHelper).putItem(any(PutItemEnhancedRequest.class));

        // Assert
        Assertions.assertThrows(AppException.class, () -> {
            repo.saveWorkflowRun(workflowRun);
        });
    }


    @Test
    void testSaveWorkflowRunException() {
        WorkflowRun workflowRun = mock(WorkflowRun.class);

        doThrow(DynamoDbException.class)
               .when(queryHelper).putItem(any(PutItemEnhancedRequest.class));

        // Assert
        Assertions.assertThrows(AppException.class, () -> {
            repo.saveWorkflowRun(workflowRun);
        });
    }


    @Test
    void testGetWorkflowRun() {
        WorkflowRunDoc doc = new WorkflowRunDoc(RUNID, PARTITION, WORKFLOWNAME, WORKFLOWNAME,
                                                1L, 1L, null, "", "");

        Mockito.when(queryHelper.getItem(any(), any()))
               .thenReturn(Optional.of(doc));

        WorkflowRun result = repo.getWorkflowRun(WORKFLOWNAME, RUNID);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(RUNID, result.getRunId());
        Assertions.assertEquals(WORKFLOWNAME, result.getWorkflowName());
    }


    @Test
    void testGetWorkflowRunWorkflowRunNotFound() {
        when(queryHelper.getItem(Mockito.eq(RUNID), Mockito.eq(PARTITION)))
               .thenReturn(Optional.empty());

        Assertions.assertThrows(WorkflowRunNotFoundException.class, () -> {
            repo.getWorkflowRun(WORKFLOWNAME, RUNID);
        });
    }


    @Test
    void testGetWorkflowRunInvalidWorkflowRun() {
        WorkflowRunDoc doc = mock(WorkflowRunDoc.class);

        when(queryHelper.getItem(Mockito.eq(RUNID), Mockito.eq(PARTITION)))
               .thenReturn(Optional.of(doc));

        when(doc.getWorkflowName()).thenReturn(WORKFLOWNAME);
        Mockito.when(doc.getDataPartitionId()).thenReturn("different-partition");

        Assertions.assertThrows(AppException.class, () -> {
            repo.getWorkflowRun(WORKFLOWNAME, RUNID);
        });

    }


    @Test
    void testGetWorkflowRunsByWorkflowName() {
        QueryPageResult<WorkflowRunDoc> result = mock(QueryPageResult.class);
        List<WorkflowRunDoc> resultItems = new ArrayList<>();
        WorkflowRunDoc doc = mock(WorkflowRunDoc.class);
        resultItems.add(doc);
        when(result.getItems()).thenReturn(resultItems);

        when(queryHelper.queryByGSI(any(GsiQueryRequest.class))).thenReturn(result);

        WorkflowRunsPage resultPage = repo.getWorkflowRunsByWorkflowName(WORKFLOWNAME, 1, CURSOR);

        Assertions.assertNotNull(resultPage);
        Assertions.assertEquals(1, resultPage.getItems().size());
    }


    @Test
    void testGetWorkflowRunsByWorkflowNameNullDocs() {

        QueryPageResult<WorkflowRunDoc> result = mock(QueryPageResult.class);
        when(result.getItems()).thenReturn(List.of());
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class)))
               .thenReturn(result);

        WorkflowRunsPage resultPage = repo.getWorkflowRunsByWorkflowName(WORKFLOWNAME, 1, CURSOR);

        Assertions.assertNotNull(resultPage);
        Assertions.assertTrue(resultPage.getItems().isEmpty());
    }

    @Test
    void testGetWorkflowRunsByWorkflowNameException() {
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class)))
               .thenThrow(new IllegalArgumentException("Test exception"));

        Assertions.assertThrows(AppException.class, () -> repo.getWorkflowRunsByWorkflowName(WORKFLOWNAME, 1, CURSOR));
    }


    @Test
    void testDeleteWorkflowRuns() {
        List<String> list = new ArrayList<>();
        list.add("id1");
        list.add("id2");

        repo.deleteWorkflowRuns(WORKFLOWNAME, list);

        verify(queryHelper, times(2)).deleteItem(anyString(), Mockito.eq(PARTITION));
    }

    @Test
    void testDeleteWorkflowRunsException() {
        doThrow(DynamoDbException.class).when(queryHelper).deleteItem(anyString(), any());
        List<String> list = List.of("id1", "id2");

        Assertions.assertThrows(AppException.class, () -> repo.deleteWorkflowRuns(WORKFLOWNAME, list));
    }


    @Test
    void testUpdateWorkflowRun() {
        WorkflowRun workflowRun = mock(WorkflowRun.class);

        WorkflowRun result = repo.updateWorkflowRun(workflowRun);

        Assertions.assertNotNull(result);
        verify(queryHelper, times(1)).putItem(any(PutItemEnhancedRequest.class));
    }


    @Test
    void testUpdateWorkflowRunNonExist() {
        WorkflowRun workflowRun = mock(WorkflowRun.class);

        doThrow(ConditionalCheckFailedException.class)
               .when(queryHelper).putItem(any(PutItemEnhancedRequest.class));

        Assertions.assertThrows(AppException.class, () -> repo.updateWorkflowRun(workflowRun));
    }


    @Test
    void testUpdateWorkflowRunException() {
        WorkflowRun workflowRun = Mockito.mock(WorkflowRun.class);

        doThrow(DynamoDbException.class)
               .when(queryHelper).putItem(any(PutItemEnhancedRequest.class));

        Assertions.assertThrows(AppException.class, () -> repo.updateWorkflowRun(workflowRun));
    }



    @Test
    void testGetAllRunInstancesOfWorkflow() {
        // Setup for first call
        QueryPageResult<WorkflowRunDoc> firstResult = mock(QueryPageResult.class);
        List<WorkflowRunDoc> firstResultItems = new ArrayList<>();
        WorkflowRunDoc doc1 = Mockito.mock(WorkflowRunDoc.class);
        firstResultItems.add(doc1);
        when(firstResult.getItems()).thenReturn(firstResultItems);
        
        // Setup for second call (no more results)
        QueryPageResult<WorkflowRunDoc> secondResult = mock(QueryPageResult.class);

        
        when(queryHelper.queryByGSI(Mockito.any(GsiQueryRequest.class)))
               .thenReturn(firstResult)
               .thenReturn(secondResult);

        List<WorkflowRun> result = repo.getAllRunInstancesOfWorkflow(WORKFLOWNAME, null);

        // assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    void testRunExistsTrue() {
        when(queryHelper.getItem(anyString(), anyString())).thenReturn(Optional.of(new WorkflowRunDoc()));

        // assert
        Assertions.assertTrue(repo.runExists(RUNID));
    }

    @Test
    void testRunExistsFalse() {
        when(queryHelper.getItem(anyString(), anyString())).thenReturn(Optional.empty());

        // assert
        Assertions.assertFalse(repo.runExists(RUNID));
    }

}