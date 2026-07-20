package org.opengroup.osdu.azure.workflow.framework.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class WorkflowRun {
  private String workflowId;
  private String runId;
  private Long startTimeStamp;
  private Long endTimeStamp;
  private String status;
  private String submittedBy;
}
