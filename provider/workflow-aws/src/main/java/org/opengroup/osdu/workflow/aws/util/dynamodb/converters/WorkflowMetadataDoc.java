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

package org.opengroup.osdu.workflow.aws.util.dynamodb.converters;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import org.opengroup.osdu.workflow.model.WorkflowMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
public class WorkflowMetadataDoc {

    private String dataPartitionId;
    private String workflowId;
    private String workflowName;
    private String description;
    private String createdBy;
    private Long creationTimestamp;
    private Long version;
    private Boolean isDeployedThroughWorkflowService;

    @DynamoDbSortKey
    @DynamoDbAttribute("dataPartitionId")
    public String getDataPartitionId() {
        return dataPartitionId;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("workflowId")
    public String getWorkflowId() {
        return workflowId;
    }

    @DynamoDbAttribute("workflowName")
    public String getWorkflowName() {
        return workflowName;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }

    @DynamoDbAttribute("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @DynamoDbAttribute("creationTimestamp")
    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    @DynamoDbAttribute("version")
    public Long getVersion() {
        return version;
    }

    @DynamoDbAttribute("isDeployedThroughWorkflowService")
    public Boolean getIsDeployedThroughWorkflowService() {
        return isDeployedThroughWorkflowService;
    }

    public static WorkflowMetadataDoc create(WorkflowMetadata workflowMetadata, String dataPartitionId) {
        return WorkflowMetadataDoc.builder()
            .dataPartitionId(dataPartitionId)
            .workflowId(workflowMetadata.getWorkflowId())
            .workflowName(workflowMetadata.getWorkflowName())
            .description(workflowMetadata.getDescription())
            .createdBy(workflowMetadata.getCreatedBy())
            .creationTimestamp(workflowMetadata.getCreationTimestamp())
            .version(workflowMetadata.getCreationTimestamp())
            .isDeployedThroughWorkflowService(false) //we dont support deployment right now
            .build();

    }

    public WorkflowMetadata convertToWorkflowMetadata() {
        return  WorkflowMetadata.builder()
            .workflowId(workflowId)
            .workflowName(workflowName)
            .description(description)
            .createdBy(createdBy)
            .creationTimestamp(creationTimestamp)
            .version(version)
            .isDeployedThroughWorkflowService(isDeployedThroughWorkflowService)
            .build();
    }


}
