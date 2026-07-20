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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
public class WorkflowRunDoc {

    private String runId;
    private String dataPartitionId;
    private String workflowId;
    private String workflowName;
    private Long startTimeStamp;
    private Long endTimeStamp;
    private WorkflowStatusType status;
    private String submittedBy;
    private String workflowEngineExecutionDate;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("runId")
    public String getRunId() {
        return runId;
    }

    @DynamoDbSortKey
    @DynamoDbSecondarySortKey(indexNames = "workflowName-tenant-index")
    @DynamoDbAttribute("dataPartitionId")
    public String getDataPartitionId() {
        return dataPartitionId;
    }

    @DynamoDbAttribute("workflowId")
    public String getWorkflowId() {
        return workflowId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "workflowName-tenant-index")
    @DynamoDbAttribute("workflowName")
    public String getWorkflowName() {
        return workflowName;
    }

    @DynamoDbAttribute("startTimeStamp")
    public Long getStartTimeStamp() {
        return startTimeStamp;
    }
    
    @DynamoDbAttribute("endTimeStamp")
    public Long getEndTimeStamp() {
        return endTimeStamp;
    }

    @DynamoDbAttribute("status")
    public WorkflowStatusType getStatus() {
        return status;
    }

    @DynamoDbAttribute("submittedBy")
    public String getSubmittedBy() {
        return submittedBy;
    }

    @DynamoDbAttribute("workflowEngineExecutionDate")
    public String getWorkflowEngineExecutionDate() {
        return workflowEngineExecutionDate;
    }

    public static WorkflowRunDoc create(WorkflowRun workflowRun, String dataPartitionId) {
        return WorkflowRunDoc.builder()
            .runId(workflowRun.getRunId())    
            .dataPartitionId(dataPartitionId)
            .workflowId(workflowRun.getWorkflowId())
            .workflowName(workflowRun.getWorkflowName())            
            .startTimeStamp(workflowRun.getStartTimeStamp())
            .endTimeStamp(workflowRun.getEndTimeStamp())
            .status(workflowRun.getStatus())
            .submittedBy(workflowRun.getSubmittedBy())
            .workflowEngineExecutionDate(workflowRun.getWorkflowEngineExecutionDate())
            .build();

    }

    public WorkflowRun convertToWorkflowRun() {
        return WorkflowRun.builder()
            .runId(runId)    
            .workflowId(workflowId)
            .workflowName(workflowName)            
            .startTimeStamp(startTimeStamp)
            .endTimeStamp(endTimeStamp)
            .status(status)
            .submittedBy(submittedBy)
            .workflowEngineExecutionDate(workflowEngineExecutionDate)
            .build();
    }
}
