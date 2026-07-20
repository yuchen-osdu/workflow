package org.opengroup.osdu.workflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AirflowGetDAGRunStatus {

  @JsonProperty("state")
  WorkflowStatusType statusType;

}
