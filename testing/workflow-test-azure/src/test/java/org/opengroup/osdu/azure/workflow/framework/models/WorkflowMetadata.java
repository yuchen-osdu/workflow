package org.opengroup.osdu.azure.workflow.framework.models;


import lombok.*;

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
  private String workflowId;
  private String workflowName;
  private String description;
  private String createdBy;
  private Long creationTimestamp;
  private Long version;
  private Map<String, Object> registrationInstructions;
}
