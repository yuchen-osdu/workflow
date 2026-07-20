package org.opengroup.osdu.workflow.provider.azure.interfaces;


import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.RegisterCustomOperatorRequest;

public interface ICustomOperatorService {
  /**
   * Creates new custom operator.
   * @param customOperatorRequest Request object describing the custom operator.
   * @return Created custom operator information.
   */
  CustomOperator registerNewOperator(RegisterCustomOperatorRequest customOperatorRequest);

  /**
   * Get all custom operators which are registered
   * @param limit Number of custom operators to return.
   * @param cursor Checkpoint information from which to fetch the next set of custom operators.
   * @return All custom operators starting from the cursor.
   */
  CustomOperatorsPage getAllOperators(Integer limit, String cursor);

  /**
   * Get registered custom operator filtered by operator id
   * @param operatorName Custom operator name.
   * @return  Custom operator information for the specified custom operator id.
   */
  CustomOperator getOperatorByName(String operatorName);
}
