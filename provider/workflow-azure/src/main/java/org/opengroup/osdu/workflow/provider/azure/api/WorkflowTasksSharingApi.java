package org.opengroup.osdu.workflow.provider.azure.api;

import io.swagger.v3.oas.annotations.Hidden;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IWorkflowTasksSharingService;
import org.opengroup.osdu.workflow.provider.azure.model.GetSignedUrlResponse;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workflow")
public class WorkflowTasksSharingApi {
  @Autowired
  private IWorkflowTasksSharingService workflowTasksSharingService;

  @Hidden
  @GetMapping("/{workflow_name}/workflowRun/{runId}/getSignedUrl")
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.ADMIN + "')")
  public GetSignedUrlResponse create(@PathVariable("workflow_name") final String workflowName,
                                     @PathVariable("runId") final String runId) {
    return workflowTasksSharingService.getSignedUrl(workflowName, runId);
  }
}
