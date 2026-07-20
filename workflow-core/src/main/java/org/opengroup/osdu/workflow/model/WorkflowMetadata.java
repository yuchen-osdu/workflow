package org.opengroup.osdu.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@Builder
@NonNull
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class WorkflowMetadata {
  @Schema(description = "System generated id, which uniquely recongnizes a workflow.", type = "string")
  private String workflowId;
  @Schema(description = "Workfow name given as input from user while deploying the workflow.", type = "string")
  private String workflowName;
  @Schema(description = "Description of workflow provided by user at time of creation of workflow.", type = "string")
  private String description;
  @Schema(description = "System captured user info who created workflow.", type = "string")
  private String createdBy;
  @Schema(description = "System date of creation of workflow.Epoch tiemstamp.", type = "integer", format = "int64")
  private Long creationTimestamp;
  @Schema(description = "Sematic versions of workflow. These numbers are assigned in increasing order and correspond to edits\\modifications to workflow definitions.", type = "integer", format = "int32")
  private Long version;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private boolean isDeployedThroughWorkflowService;
  @Schema(description = "Workfow registration instructions which could contains:\n" +
      "\n" +
      "Name of already registered Airflow DAG\n" +
      "Cotent of python DAG file\n" +
      "etc By default this is Airflow DAG named workflowName")
  private Map<String, Object> registrationInstructions;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private boolean isSystemWorkflow;

  @JsonIgnore
  public boolean isDeployedThroughWorkflowService() {
    return isDeployedThroughWorkflowService;
  }

  public void setIsDeployedThroughWorkflowService(boolean isDeployedThroughWorkflowService) {
    this.isDeployedThroughWorkflowService = isDeployedThroughWorkflowService;
  }

  @JsonIgnore
  public boolean isSystemWorkflow() {
    return isSystemWorkflow;
  }

  public void setIsSystemWorkflow(boolean isSystemWorkflow) {
    this.isSystemWorkflow = isSystemWorkflow;
  }
}
