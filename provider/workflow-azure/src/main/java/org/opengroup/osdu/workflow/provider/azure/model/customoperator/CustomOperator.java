package org.opengroup.osdu.workflow.provider.azure.model.customoperator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomOperator {
  private String id;
  private String name;
  private String className;
  private String description;
  private String createdBy;
  private Long createdAt;
  private List<CustomOperatorProperty> properties;
}
