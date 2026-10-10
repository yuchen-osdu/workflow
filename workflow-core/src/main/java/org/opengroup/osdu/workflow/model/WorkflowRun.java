package org.opengroup.osdu.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "Reperesent one workflow run.")
public class WorkflowRun {
  @Schema(description = "Workflow id for the workflow", type = "string")
  private String workflowId;
  @Schema(description = "Workflow id for the workflow", type = "string")
  private String workflowName;
  @Schema(description = "Run id for the workflow", type = "string")
  private String runId;
  @Schema(description = "Start timestamp of the workflow run.Epoch time stamp", type = "integer", format = "int64")
  private Long startTimeStamp;
  @Schema(description = "End timestamp of the workflow run.Epoch timestamp", type = "integer", format = "int64")
  private Long endTimeStamp;
  @Schema(description = "Task execution status")
  private WorkflowStatusType status;
  @Schema(description = "System captured user details which triggered the run.", type = "string")
  private String submittedBy;
  private String workflowEngineExecutionDate;
  /**
   * Airflow engine that owns this run (e.g. airflow2, airflow3). Persisted for deterministic
   * status/log routing, but intentionally NOT serialized in API responses so the public contract
   * does not expose the internal engine choice.
   */
  @JsonIgnore
  private String engineVersion;
}
