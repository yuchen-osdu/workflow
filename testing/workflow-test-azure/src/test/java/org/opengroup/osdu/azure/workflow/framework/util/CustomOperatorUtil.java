package org.opengroup.osdu.azure.workflow.framework.util;

public class CustomOperatorUtil {
  public static String getUniqueOperatorName(String operatorName) {
    return String.format("%s_%d", operatorName, System.currentTimeMillis());
  }
}
