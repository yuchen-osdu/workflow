package org.opengroup.osdu.workflow.provider.azure.fileshare;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("osdu.azure.fileshare")
@Configuration
@Getter
@Setter
public class FileShareConfig {
  @Deprecated
  private String shareName;
  private String airflow2ShareName;
  private String dagsFolder;
  private String customOperatorsFolder;
}
