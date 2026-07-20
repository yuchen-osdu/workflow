package org.opengroup.osdu.workflow.provider.azure.model.customoperator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterCustomOperatorRequest {
  @NotEmpty(message = "Name should not be null or empty")
  private String name;

  @NotEmpty(message = "Class Name should not be null or empty")
  private String className;

  @NotNull
  private String description;

  @NotEmpty(message = "Content should not be null or empty")
  private String content;

  @NotNull
  private List<CustomOperatorProperty> properties;
}
