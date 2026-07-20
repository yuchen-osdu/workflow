package org.opengroup.osdu.workflow.provider.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("osdu.azure.cosmosdb")
@Configuration
@Getter
@Setter
public class CosmosConfig {
  private String database;
  private String ingestionStrategyCollection;
  private String workflowStatusCollection;
  private String workflowMetadataCollection;
  private String workflowRunCollection;
  private String workflowTasksSharingCollection;
  private String customOperatorCollection;
  private String systemdatabase;
}
