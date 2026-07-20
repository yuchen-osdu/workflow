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
public class CustomOperatorsPage {
  private List<CustomOperator> items;
  private String cursor;
}
