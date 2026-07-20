package org.opengroup.osdu.workflow.provider.azure.api;

import io.swagger.v3.oas.annotations.Hidden;
import org.opengroup.osdu.workflow.model.WorkflowRole;
import org.opengroup.osdu.workflow.provider.azure.interfaces.ICustomOperatorService;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.RegisterCustomOperatorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/customOperator")
public class CustomOperatorApi {
  @Autowired
  private ICustomOperatorService customOperatorService;

  /**
   * API to register a new custom operator
   * @param request Request object which has information to create new custom operator.
   * @return Information about created custom operator.
   */
  @Hidden
  @PostMapping
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.ADMIN + "')")
  public CustomOperator registerCustomOperator(
      @RequestBody @Valid RegisterCustomOperatorRequest request) {
    return customOperatorService.registerNewOperator(request);
  }

  /**
   * Returns all custom operators existing in the system (with a maximum limit of 50)
   * @param limit Number of custom operators to return. If not set, default is 50
   * @param cursor Checkpoint information from which to fetch the next set of custom operators
   * @return Information about all the custom operators
   */
  @Hidden
  @GetMapping
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.ADMIN + "', '"
      + WorkflowRole.CREATOR + "', '" + WorkflowRole.VIEWER + "')")
  public CustomOperatorsPage getAllCustomOperators(
      @RequestParam(value = "limit", required = false, defaultValue = "50") final Integer limit,
      @RequestParam(value = "cursor", required = false) final String cursor) {
    return customOperatorService.getAllOperators(limit, cursor);
  }

  /**
   * Returns custom operator by ID
   * @param customOperatorName Name of custom operator
   * @return Custom Operator by ID
   */
  @Hidden
  @GetMapping("/{custom_operator_name}")
  @PreAuthorize("@authorizationFilter.hasPermission('" + WorkflowRole.ADMIN + "', '"
      + WorkflowRole.CREATOR + "', '" + WorkflowRole.VIEWER + "')")
  public CustomOperator getCustomOperatorByName(@PathVariable("custom_operator_name")
                                                 String customOperatorName) {
    return customOperatorService.getOperatorByName(customOperatorName);
  }
}
