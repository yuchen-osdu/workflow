package org.opengroup.osdu.workflow.provider.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("osdu.azure.active-dag-runs")
@Getter
@Setter
public class ActiveDagRunsConfig {
  private int threshold;
}
