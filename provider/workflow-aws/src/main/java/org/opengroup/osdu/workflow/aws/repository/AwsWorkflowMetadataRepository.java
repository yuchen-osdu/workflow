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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import org.opengroup.osdu.core.aws.v2.dynamodb.util.RequestBuilderUtil;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.aws.util.dynamodb.converters.WorkflowMetadataDoc;
import org.opengroup.osdu.workflow.exception.ResourceConflictException;
import org.opengroup.osdu.workflow.exception.WorkflowNotFoundException;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;

@Repository
@RequestScope
public class AwsWorkflowMetadataRepository implements IWorkflowMetadataRepository {


  private final IDynamoDBQueryHelperFactory queryHelperFactory;
    private final DpsHeaders headers;
    private final String workflowMetadataTableParameterRelativePath;
    private final DynamoDBQueryHelper<WorkflowMetadataDoc> queryHelper;

    @Autowired
    public AwsWorkflowMetadataRepository(IDynamoDBQueryHelperFactory queryHelperFactory,
            @Value("${aws.dynamodb.workflowMetadataTable.ssm.relativePath}") String workflowMetadataTableParameterRelativePath,
            DpsHeaders headers) {
        this.queryHelperFactory = queryHelperFactory;
        this.headers = headers;
        this.workflowMetadataTableParameterRelativePath = workflowMetadataTableParameterRelativePath;
      this.queryHelper = getWorkflowMetadataRepositoryQueryHelper();
    }

    private DynamoDBQueryHelper<WorkflowMetadataDoc> getWorkflowMetadataRepositoryQueryHelper() {
        return queryHelperFactory.createQueryHelper(headers,
                workflowMetadataTableParameterRelativePath, WorkflowMetadataDoc.class);
    }

    private String getDataPartitionId() {
        return headers.getPartitionIdWithFallbackToAccountId();
    }

    @Override
    public WorkflowMetadata createWorkflow(WorkflowMetadata workflowMetadata) {
        String dataPartitionId = getDataPartitionId();
        workflowMetadata.setWorkflowId(generateWorkflowId(workflowMetadata.getWorkflowName(), dataPartitionId));

        WorkflowMetadataDoc doc = WorkflowMetadataDoc.create(workflowMetadata, dataPartitionId);

        try {
            PutItemEnhancedRequest<WorkflowMetadataDoc> request = PutItemEnhancedRequest.builder(WorkflowMetadataDoc.class)
                    .item(doc)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_not_exists(workflowId)")
                            .build())
                    .build();

            queryHelper.putItem(request);
            return doc.convertToWorkflowMetadata();
        } catch (ConditionalCheckFailedException e) {
            throw new ResourceConflictException(workflowMetadata.getWorkflowName(),
                    "Workflow with same name already exists");
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error creating workflow",
                    e.getMessage());
        }
    }

    @Override
    public WorkflowMetadata getWorkflow(String workflowName) {
        String dataPartitionId = getDataPartitionId();
        String workflowId = generateWorkflowId(workflowName, dataPartitionId);

        try {
            Optional<WorkflowMetadataDoc> docOptional = queryHelper.getItem(workflowId, dataPartitionId);

            if (docOptional.isEmpty()) {
                throw new WorkflowNotFoundException(String.format("Workflow: '%s' not found", workflowName));
            }

            return docOptional.get().convertToWorkflowMetadata();
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error retrieving workflow",
                    e.getMessage());
        }
    }

    @Override
    public void deleteWorkflow(String workflowName) {
        String dataPartitionId = getDataPartitionId();
        String workflowId = generateWorkflowId(workflowName, dataPartitionId);

        try {
            queryHelper.deleteItem(workflowId, dataPartitionId);
        } catch (InternalServerErrorException e) {
            throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Service error occurred",
                    e.getMessage());
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error deleting workflow",
                    e.getMessage());
        }
    }

    @Override
    public List<WorkflowMetadata> getAllWorkflowForTenant(String prefix) {
        try {
            ScanEnhancedRequest request = buildScanRequest(prefix);
            QueryPageResult<WorkflowMetadataDoc> pageResult = queryHelper.scanPage(request);
            List<WorkflowMetadataDoc> docs = (pageResult != null) ? pageResult.getItems() : Collections.emptyList();

            return docs.stream()
                    .map(WorkflowMetadataDoc::convertToWorkflowMetadata)
                       .collect(Collectors.toList()); //NOSONAR - mutable list required by calling code
        } catch (DynamoDbException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error listing workflows",
                    e.getMessage());
        }
    }

    private ScanEnhancedRequest buildScanRequest(String prefix) {
        String filterExpression = "dataPartitionId = :partitionId";
        AttributeValue partitionValue = AttributeValue.builder().s(getDataPartitionId()).build();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":partitionId", partitionValue);

        if (StringUtils.isNotBlank(prefix)) {
            filterExpression += " AND begins_with(workflowName, :workflowNamePrefix)";
            AttributeValue prefixValue = AttributeValue.builder().s(prefix).build();
            expressionValues.put(":workflowNamePrefix", prefixValue);
        }

        return RequestBuilderUtil.ScanRequestBuilder.forScan(WorkflowMetadataDoc.class)
                .filterExpression(filterExpression, expressionValues)
                .build();
    }

    private String generateWorkflowId(String workflowName, String dataPartitionId) {
        return String.format("%s:%s", dataPartitionId, workflowName);
    }
}
