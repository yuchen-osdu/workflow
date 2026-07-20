/*
 *  Copyright 2020-2025 Google LLC
 *  Copyright 2020-2025 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowRunExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workflow")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "osdu.airflow.version2", havingValue = "true", matchIfMissing = false)
public class RunDetailsApi {

  private final IWorkflowRunExtension workflowRunExtension;

  @Operation(
      summary = "${workflowRunApi.latestInfo.summary}",
      description = "${workflowRunApi.latestInfo.description}",
      security = {@SecurityRequirement(name = "Authorization")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Workflow execution details of the latest task.",
            content = {@Content(schema = @Schema(implementation = Object.class))}),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "403",
            description = "User not authorized to perform the action.",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "502",
            description = "Bad Gateway",
            content = {@Content(schema = @Schema(implementation = AppError.class))}),
        @ApiResponse(
            responseCode = "503",
            description = "Service Unavailable",
            content = {@Content(schema = @Schema(implementation = AppError.class))})
      })
  @GetMapping(
      value = "/{workflow_name}/workflowRun/{runId}/latestInfo",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "@authorizationFilter.hasPermission('"
          + WorkflowRole.VIEWER
          + "','"
          + WorkflowRole.CREATOR
          + "','"
          + WorkflowRole.ADMIN
          + "')"
          + "and @authorizationWorkflowFilter.isCreatorOf(#workflowName, #runId)")
  public Object getWorkflowRunDetails(
      @PathVariable("workflow_name") final String workflowName,
      @PathVariable("runId") final String runId) {
    return workflowRunExtension.getLatestTaskDetails(workflowName, runId);
  }
}
