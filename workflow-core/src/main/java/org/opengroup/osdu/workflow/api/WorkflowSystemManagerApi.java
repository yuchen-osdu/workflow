package org.opengroup.osdu.workflow.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workflow/system")
@Tag(name = "workflow-system-manager-api", description = "Workflow System Manager related endpoints")
public class WorkflowSystemManagerApi {
  @Autowired
  private IWorkflowManagerService workflowManagerService;

  /**
   * API to create a system workflow.
   * @param request Request object which has information to create workflow.
   * @return Workflow metadata.
   */
  @Operation(summary = "${workflowManagerApi.deployWorkflow.summary}", description = "${workflowManagerApi.deployWorkflow.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow created | updated successfully", content = { @Content(schema = @Schema(implementation = WorkflowMetadata.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @PostMapping
  @PreAuthorize("@authorizationFilter.hasRootPermission()")
  public WorkflowMetadata createSystemWorkflow(@RequestBody @Valid final  CreateWorkflowRequest request) {
    return workflowManagerService.createSystemWorkflow(request);
  }

  /**
   * Deletes system workflow by workflowName
   * @param workflowName Name of the workflow which needs to be deleted.
   */
  @Operation(summary = "${workflowManagerApi.deleteWorkflow.summary}", description = "${workflowManagerApi.deleteWorkflow.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Workflow deleted successfully."),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @DeleteMapping("/{workflow_name}")
  @PreAuthorize("@authorizationFilter.hasRootPermission()")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSystemWorkflowById(@PathVariable("workflow_name") final String workflowName) {
    workflowManagerService.deleteSystemWorkflow(workflowName);
  }
}
