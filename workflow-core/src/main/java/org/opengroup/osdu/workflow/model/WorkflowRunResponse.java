package org.opengroup.osdu.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor

public class WorkflowRunResponse {
  @Schema(description = "Workflow id for the workflow", type = "string")
  private String workflowId;
  @Schema(description = "Run id for the workflow", type = "string")
  private String runId;
  @Schema(description = "Start timestamp of the workflow run.Epoch time stamp", type = "integer", format = "int64")
  private Long startTimeStamp;
  @Schema(description = "End timestamp of the workflow run.Epoch timestamp", type = "integer", format = "int64")
  private Long endTimeStamp;
  @Schema(description = "Task execution status", type = "string", allowableValues = {"INPROGRESS", "PARTIAL_SUCCESS", "SUCCESS", "FAILED", "SUBMITTED"})
  private WorkflowStatusType status;
  @Schema(description = "System captured user details which triggered the run", type = "string")
  private String submittedBy;
}

