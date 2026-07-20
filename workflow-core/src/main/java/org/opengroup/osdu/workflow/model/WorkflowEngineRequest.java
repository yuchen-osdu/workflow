package org.opengroup.osdu.workflow.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowEngineRequest {

  private String runId;
  private String workflowId;
  private String workflowName;
  private String dagName;
  private String workflowEngineExecutionDate;
  @Builder.Default
  private long executionTimeStamp = System.currentTimeMillis();
  private boolean isDeployedThroughWorkflowService;
  private final boolean isSystemWorkflow;

}
