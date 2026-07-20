/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.api;

import jakarta.annotation.security.PermitAll;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

@RestController
@RequestScope
@Tag(name = "health", description = "Health related endpoints")
public class HealthCheckApi {

  @Operation(summary = "${healthCheckApi.livenessCheck.summary}",
      description = "${healthCheckApi.livenessCheck.description}", tags = {"health"})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow service is alive", content = {@Content(schema = @Schema(implementation = String.class))})
  })
  @PermitAll
  @GetMapping("/liveness_check")
  public ResponseEntity<String> livenessCheck() {
    return new ResponseEntity<>("Workflow service is alive", HttpStatus.OK);
  }

  @Operation(summary = "${healthCheckApi.readinessCheck.summary}",
      description = "${healthCheckApi.readinessCheck.description}", tags = {"health"})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Workflow service is ready", content = {@Content(schema = @Schema(implementation = String.class))})
  })
  @PermitAll
  @GetMapping("/readiness_check")
  public ResponseEntity<String> readinessCheck() {
    return new ResponseEntity<>("Workflow service is ready", HttpStatus.OK);
  }

}
