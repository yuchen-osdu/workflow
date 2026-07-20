package org.opengroup.osdu.workflow.provider.azure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorProperty;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomOperatorDoc {
  private String id;
  private String partitionKey;
  private String name;
  private String className;
  private String description;
  private String createdBy;
  private Long createdAt;
  private List<CustomOperatorProperty> properties;
}
