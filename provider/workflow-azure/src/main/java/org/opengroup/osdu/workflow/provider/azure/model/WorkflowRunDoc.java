package org.opengroup.osdu.workflow.provider.azure.model;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkflowRunDoc {
  private String id;
  private String partitionKey;
  private String runId;
  private String workflowName;
  private Long startTimeStamp;
  private Long endTimeStamp;
  private String status;
  private String submittedBy;
  private String workflowEngineExecutionDate;
}
