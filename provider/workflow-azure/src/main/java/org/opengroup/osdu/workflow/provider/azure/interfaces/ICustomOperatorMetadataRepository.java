package org.opengroup.osdu.workflow.provider.azure.interfaces;


import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperator;
import org.opengroup.osdu.workflow.provider.azure.model.customoperator.CustomOperatorsPage;

public interface ICustomOperatorMetadataRepository {
  /**
   * Creates custom operator metadata record in persistence store.
   * @param customOperator object representing custom operator.
   * @return created custom operator object.
   */
  CustomOperator saveMetadata(CustomOperator customOperator);

  /**
   * Get custom operator metadata by custom operator name/id.
   * @param operatorName custom operator name.
   * @return Custom Operator object associated to given id.
   */
  CustomOperator getMetadataByCustomOperatorName(String operatorName);

  /**
   * Get all custom operators metadata which are created.
   * @param limit Number of custom operators to retrieve.
   * @param cursor Checkpoint information from which to fetch the next set of custom operators.
   * @return an object which contains the list of custom operators and a continuation cursor
   */
  CustomOperatorsPage getMetadataForAllCustomOperators(Integer limit, String cursor);
}
