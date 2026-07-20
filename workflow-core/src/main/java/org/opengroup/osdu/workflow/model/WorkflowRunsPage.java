package org.opengroup.osdu.workflow.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkflowRunsPage {
  private List<WorkflowRun> items;
  private String cursor;
}
