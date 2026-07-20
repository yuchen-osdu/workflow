package org.opengroup.osdu.workflow.provider.azure.config;

import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.azure.cache.AirflowConfigCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AirflowConfigResolver {
  private static final String CACHE_KEY_FORMAT = "%s-airflow-config";

  @Autowired
  private PartitionServiceClient partitionService;

  @Autowired
  private AirflowConfigCache airflowConfigCache;

  @Autowired
  private AirflowConfig defaultAirflowConfig;

  public AirflowConfig getAirflowConfig(String dataPartitionId) {
    String cacheKey = String.format(CACHE_KEY_FORMAT, dataPartitionId);
    AirflowConfig airflowConfig = getAirflowConfigFromCache(cacheKey);
    if(airflowConfig == null) {
      PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

      if(pi.getAirflowEnabled()) {
        airflowConfig = createAirflowConfigFromPartitionInfo(pi);
      } else {
        airflowConfig = defaultAirflowConfig;
      }
      this.airflowConfigCache.put(cacheKey, airflowConfig);
    }
    return airflowConfig;
  }

  public AirflowConfig getSystemAirflowConfig() {
    return defaultAirflowConfig;
  }

  private AirflowConfig getAirflowConfigFromCache(String cacheKey) {
    if (this.airflowConfigCache.containsKey(cacheKey)) {
      return this.airflowConfigCache.get(cacheKey);
    }
    return null;
  }

  private AirflowConfig createAirflowConfigFromPartitionInfo(PartitionInfoAzure pi) {
    AirflowConfig airflowConfig = new AirflowConfig();
    airflowConfig.setUrl(pi.getAirflowEndpoint());
    airflowConfig.setUsername(pi.getAirflowUsername());
    airflowConfig.setPassword(pi.getAirflowPassword());

    return airflowConfig;
  }
}
