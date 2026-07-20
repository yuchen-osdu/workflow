package org.opengroup.osdu.workflow.provider.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "azure.workflow.trigger")
@Getter
@Setter
public class TriggerWorkflowConfig {
  private int maxRequestSize;
  private static final int KB_TO_Bytes_CONVERSION_FACTOR = 1000;

  public int getMaxRequestSizeInBytes() {
    return maxRequestSize*KB_TO_Bytes_CONVERSION_FACTOR;
  }
}

