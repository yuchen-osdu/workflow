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
  /**
   * Airflow engine that owns this request (see
   * {@link org.opengroup.osdu.workflow.model.AirflowEngineVersions}). Set to the configured default
   * engine on trigger and to the run's persisted engine on status/log lookups. When {@code null},
   * providers fall back to the Airflow 2 engine for backward compatibility.
   */
  private String engineVersion;

}
