package org.opengroup.osdu.azure.workflow.framework.util;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.azure.workflow.framework.consts.DefaultVariable;

@Slf4j
public class WorkflowUtil {

  public static String getUniqueWorkflowName(String dagName) {
    return String.format("%s_%d", dagName, System.currentTimeMillis());
  }

  public static void waitForDAGActivation(String... dagNames) {
    int secondsToWait = Integer.parseInt(DefaultVariable
        .getEnvironmentVariableOrDefaultKey(DefaultVariable.DAG_ACTIVATION_TIME));
    try {
      log.info("Waiting for DAG activation for {} seconds", secondsToWait);
      Thread.sleep(secondsToWait * 1000);
    } catch (InterruptedException e) {
      log.error("Error while waiting for dag to become active");
    }
    log.info("DAG activation successful");
  }
}
