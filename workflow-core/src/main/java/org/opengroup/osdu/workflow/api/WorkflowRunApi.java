package org.opengroup.osdu.workflow.api;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.opengroup.osdu.workflow.model.TriggerWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.model.UpdateWorkflowRunRequest;
import org.opengroup.osdu.workflow.model.WorkflowRunResponse;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.opengroup.osdu.core.common.model.http.AppError;

@RestController
@RequestMapping("/v1/workflow")
@Tag(name = "workflow-run-api", description = "WorkflowRun related endpoints")
public class WorkflowRunApi {
  @Autowired
  private IWorkflowRunService workflowRunService;

  /**
   * API to trigger a workflow.
   * @param workflowName Workflow to trigger.
   * @param request Request object which has information to trigger workflow.
   * @return Information about workflow run.
   */
  @Operation(summary = "${workflowRunApi.workflowRun.summary}", description = "${workflowRunApi.workflowRun.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow successully triggered", content = { @Content(schema = @Schema(implementation = WorkflowRunResponse.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @PostMapping(value = "/{workflow_name}/workflowRun", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.CREATOR + "', '" + WorkflowRole.ADMIN + "')")
  public WorkflowRunResponse triggerWorkflow(@PathVariable("workflow_name") String workflowName,
      @RequestBody TriggerWorkflowRequest request) {
    return workflowRunService.triggerWorkflow(workflowName, request);
  }

  /**
   * Returns Information about workflow run. based on workflowName, runId
   * @param workflowName Name of the workflow for which workflowRun should be checked.
   * @param runId Id of the workflowRun for which metadata should be retrieved.
   * @return Information about workflow run.
   */
  @Operation(summary = "${workflowRunApi.workflowRunById.summary}", description = "${workflowRunApi.workflowRunById.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow execution detail", content = { @Content(schema = @Schema(implementation = WorkflowRunResponse.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @GetMapping(value = "/{workflow_name}/workflowRun/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.VIEWER + "', '" + WorkflowRole.CREATOR + "', '" + WorkflowRole.ADMIN + "')")
  public WorkflowRunResponse getWorkflowRunById(@PathVariable("workflow_name") final String workflowName,
      @PathVariable("runId") final String runId) {
    return workflowRunService.getWorkflowRunByName(workflowName, runId);
  }

  /**
   * Get all run instances of a workflow.
   * @param workflowName Workflow to trigger.
   * @return Information list about workflow run.
   */
  @Operation(summary = "${workflowRunApi.getAllWorkflowRuns.summary}", description = "${workflowRunApi.getAllWorkflowRuns.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "History of workflow runs", content = { @Content(schema = @Schema(implementation = WorkflowRun.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @GetMapping(value = "/{workflow_name}/workflowRun", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.VIEWER + "', '" + WorkflowRole.CREATOR + "', '" + WorkflowRole.ADMIN + "')")
  public List<WorkflowRun> getAllRunInstances(
      @PathVariable("workflow_name") String workflowName,
      @RequestParam Map<String, Object> params) {
    return workflowRunService.getAllRunInstancesOfWorkflow(workflowName, params);
  }

  /**
   * Update the workflow run instance. based on workflowName, runId
   * @param workflowName Name of the workflow for which workflowRun should be checked.
   * @param runId Id of the workflowRun for which metadata should be retrieved.
   * @return Information about workflow run.
   */
  @Operation(summary = "${workflowRunApi.updateWorkflowRun.summary}", description = "${workflowRunApi.updateWorkflowRun.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow updated successfully", content = { @Content(schema = @Schema(implementation = WorkflowRunResponse.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @PutMapping(value = "/{workflow_name}/workflowRun/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.VIEWER + "', '" + WorkflowRole.CREATOR + "')")
  public WorkflowRunResponse updateWorkflowRun(
      @PathVariable("workflow_name") String workflowName,
      @PathVariable("runId") String runId,
      @RequestBody UpdateWorkflowRunRequest body) {
    return workflowRunService.updateWorkflowRunStatus(workflowName, runId, body.getStatus());
  }
}
