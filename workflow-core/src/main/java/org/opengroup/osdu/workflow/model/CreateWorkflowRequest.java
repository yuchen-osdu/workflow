package org.opengroup.osdu.workflow.model;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class CreateWorkflowRequest {
  @Schema(description = "Workfow name given as input from user while deploying the workflow", type = "string")
  private String workflowName;
  @Schema(description = "Description of workflow provided by user at time of creation of workflow", type = "string")
  private String description;

  @Schema(description = "Workfow registration instructions which could contains:\n" +
      "\n" +
      "Name of already registered Airflow DAG\n" +
      "Content of python DAG file\n" +
      "etc By default this is Airflow DAG named workflowName")
  @NotNull(message = "registrationInstructions can not be null")
  private Map<String, Object> registrationInstructions;
}
