package org.opengroup.osdu.workflow.api;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.workflow.model.CreateWorkflowRequest;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.model.WorkflowRunResponse;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowManagerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workflow")
@Tag(name = "workflow-manager-api", description = "WorkflowManager related endpoints")
public class WorkflowManagerApi {
  @Autowired
  private IWorkflowManagerService workflowManagerService;

  /**
   * API to create a workflow.
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
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = CreateWorkflowRequest.class),
          examples = @ExampleObject(name = "ExternalWorkflowExample", ref = "#/components/examples/ExternalWorkflowExample")
      ))
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.ADMIN + "')")
  public WorkflowMetadata create(@RequestBody @Valid final CreateWorkflowRequest request) {
    return workflowManagerService.createWorkflow(request);
  }

  /**
   * Returns workflow metadata based on workflowName
   * @param workflowName Name of the workflow for which metadata should be retrieved.
   * @return Workflow metadata
   */
  @Operation(summary = "${workflowManagerApi.viewWorkflow.summary}", description = "${workflowManagerApi.viewWorkflow.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow Details", content = { @Content(schema = @Schema(implementation = WorkflowMetadata.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @GetMapping(value = "/{workflow_name}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.VIEWER + "','" + WorkflowRole.CREATOR + "','" + WorkflowRole.ADMIN + "')")
  public  WorkflowMetadata getWorkflowByName(@PathVariable("workflow_name") final String workflowName) {
    return workflowManagerService.getWorkflowByName(workflowName);
  }

  /**
   * Deletes workflow by workflowName
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
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.ADMIN + "')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteWorkflowById(@PathVariable("workflow_name") final String workflowName) {
    workflowManagerService.deleteWorkflow(workflowName);
  }

  /**
   * Get List all the workflows for the tenant.
   * @param prefix Filter workflow names which start with the full prefix specified.
   */
  @Operation(summary = "${workflowManagerApi.listAllWorkflow.summary}", description = "${workflowManagerApi.listAllWorkflow.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "List of all the workflows", content = { @Content(schema = @Schema(implementation = WorkflowMetadata.class)) }),
      @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "409", description = "A Workflow with the given name already exists.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
      @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
  })
  @GetMapping
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.VIEWER + "','" + WorkflowRole.CREATOR + "','" + WorkflowRole.ADMIN + "')")
  public List<WorkflowMetadata> getAllWorkflowsForTenant(
      @RequestParam(required = false) String prefix) {
    return workflowManagerService.getAllWorkflowForTenant(prefix);
  }
}

