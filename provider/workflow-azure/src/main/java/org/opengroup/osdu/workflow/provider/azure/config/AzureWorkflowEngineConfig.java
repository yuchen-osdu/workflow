package org.opengroup.osdu.workflow.provider.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("osdu.azure.airflow")
@Configuration
@Setter
@Getter
public class AzureWorkflowEngineConfig {
  private Boolean isDPAirflowUsedForSystemDAG;

  private Boolean ignoreDagContent;

  private Boolean ignoreCustomOperatorContent;
}
