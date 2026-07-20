package org.opengroup.osdu.workflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerWorkflowResponse {

  @JsonProperty("execution_date")
  private String executionDate;

  @JsonProperty("message")
  private String message;

  @JsonProperty("run_id")
  private String runId;
}
