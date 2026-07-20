package org.opengroup.osdu.workflow.model;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class TriggerWorkflowRequest {
  @Schema(description = "Optional. Explicit setting up workflow run id.", type = "string")
  private String runId;
  @Schema(description = "Map to configure workflow speciffic key value pairs")
  private Map<String, Object> executionContext = new HashMap<>();
}
