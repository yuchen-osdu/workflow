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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.GsiQueryRequest;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import org.opengroup.osdu.core.aws.v2.dynamodb.util.RequestBuilderUtil;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.aws.config.AwsServiceConfig;
import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.WorkflowRunDoc;
import org.opengroup.osdu.workflow.exception.WorkflowRunNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowRunsPage;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Repository
@RequestScope
@Slf4j
public class AwsWorkflowRunRepository implements IWorkflowRunRepository {

    private static final String WORKFLOWRUN_HASHKEY = "runId";
    private static final String GSI_INDEX_NAME = "workflowName-tenant-index";

    private final AwsServiceConfig config;
    private final DpsHeaders headers;
    private final DynamoDBQueryHelper<WorkflowRunDoc> queryHelper;

    @Autowired
    public AwsWorkflowRunRepository(
            IDynamoDBQueryHelperFactory queryHelperFactory,
            @Value("${aws.dynamodb.workflowRunTable.ssm.relativePath}") String workflowRunTableParameterRelativePath,
            DpsHeaders headers, AwsServiceConfig config) {
        this.headers = headers;
        this.queryHelper = queryHelperFactory.createQueryHelper(
                headers,
                workflowRunTableParameterRelativePath,
                WorkflowRunDoc.class);
        this.config = config;
    }

    @Override
    public WorkflowRun saveWorkflowRun(WorkflowRun workflowRun) {
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();
        WorkflowRunDoc doc = WorkflowRunDoc.create(workflowRun, dataPartitionId);

        try {
            PutItemEnhancedRequest<WorkflowRunDoc> request = PutItemEnhancedRequest.builder(WorkflowRunDoc.class)
                    .item(doc)
                    .conditionExpression(Expression.builder()
                            .expression(String.format("attribute_not_exists(%s)", WORKFLOWRUN_HASHKEY))
                            .build())
                    .build();
            
            queryHelper.putItem(request);
            return workflowRun;
        } catch (ConditionalCheckFailedException e) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), "Cannot save duplicate runId");
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Failed to save workflowRun to db");
        }
    }

    @Override
    public WorkflowRun getWorkflowRun(String workflowName, String runId) {
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();

        if (!isValidWorkflowRun(runId, workflowName, dataPartitionId)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Run id's workflow/data-partition-id doesn't match workflow/data-partition-id in request");
        }

        Optional<WorkflowRunDoc> docOptional = queryHelper.getItem(runId, dataPartitionId);

        if (docOptional.isEmpty()) {
            throw new WorkflowRunNotFoundException(
                    String.format("Workflow Run: '%s' for Workflow '%s' not found", runId, workflowName));
        }

        return docOptional.get().convertToWorkflowRun();
    }


    @Override
    public WorkflowRunsPage getWorkflowRunsByWorkflowName(String workflowName, Integer limit, String cursor) {
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();

        WorkflowRunDoc queryDoc = WorkflowRunDoc.builder()
                                                .workflowName(workflowName)
                                                .dataPartitionId(dataPartitionId)
                                                .build();

        QueryPageResult<WorkflowRunDoc> result;

        try {
            // Build QueryEnhancedRequest with RequestBuilderUtil
            GsiQueryRequest<WorkflowRunDoc> queryRequest =
                RequestBuilderUtil.QueryRequestBuilder.forQuery(queryDoc, GSI_INDEX_NAME, WorkflowRunDoc.class)
                    .limit(limit)
                    .cursor(cursor)
                    .buildGsiRequest();

            result = queryHelper.queryByGSI(queryRequest);

        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                   HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                   "Failed to query workflow runs by name");

        }

        List<WorkflowRun> items;

        if (result.getItems() != null &&  (!result.getItems().isEmpty())) {
            items = result.getItems().stream()
                        .map(WorkflowRunDoc::convertToWorkflowRun)
                        .toList();
        }
        else {
            items = new ArrayList<>();
        }

        return WorkflowRunsPage.builder()
                               .cursor(result.getNextCursor())
                               .items(items)
                               .build();
    }

    @Override
    public void deleteWorkflowRuns(String workflowName, List<String> runIds) {
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();

        try {
            for (String runId : runIds) {
                queryHelper.deleteItem(runId, dataPartitionId);
            }
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Failed to delete workflow runs");
        }
    }

    @Override
    public WorkflowRun updateWorkflowRun(WorkflowRun workflowRun) {
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();
        WorkflowRunDoc doc = WorkflowRunDoc.create(workflowRun, dataPartitionId);

        try {
            PutItemEnhancedRequest<WorkflowRunDoc> request = PutItemEnhancedRequest.builder(WorkflowRunDoc.class)
                    .item(doc)
                    .conditionExpression(Expression.builder()
                            .expression(String.format("attribute_exists(%s)", WORKFLOWRUN_HASHKEY))
                            .build())
                    .build();
            
            queryHelper.putItem(request);
            return workflowRun;
        } catch (ConditionalCheckFailedException e) {
            throw new AppException(HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    "WorkflowRun not found, cannot update");
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Failed to save workflowRun to db");
        }
    }

    @Override
    public List<WorkflowRun> getAllRunInstancesOfWorkflow(String workflowName, Map<String, Object> params) {
        String cursor = null;
        List<WorkflowRun> runs = new ArrayList<>();

        do {
            WorkflowRunsPage page = getWorkflowRunsByWorkflowName(workflowName, 100, cursor);
            runs.addAll(page.getItems());
            cursor = page.getCursor();
        } while (cursor != null);

        return runs;
    }

    public boolean runExists(String runId) {
        String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();
        return queryHelper.getItem(runId, dataPartitionId).isPresent();
    }

    private boolean isValidWorkflowRun(String runId, String workflowName, String dataPartitionId) {
        Optional<WorkflowRunDoc> docOptional = queryHelper.getItem(runId, dataPartitionId);

        if (docOptional.isPresent()) {
            WorkflowRunDoc doc = docOptional.get();
            return doc.getWorkflowName().equals(workflowName) && 
                   doc.getDataPartitionId().equals(dataPartitionId);
        }
        
        return true;
    }
}
