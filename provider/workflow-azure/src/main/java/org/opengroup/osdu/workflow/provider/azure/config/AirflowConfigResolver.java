package org.opengroup.osdu.workflow.provider.azure.config;

import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.azure.cache.AirflowConfigCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AirflowConfigResolver {
  private static final String CACHE_KEY_FORMAT = "%s-airflow-config";

  @Autowired
  private PartitionServiceClient partitionService;

  @Autowired
  private AirflowConfigCache airflowConfigCache;

  @Autowired
  @Qualifier("airflowConfig")
  private AirflowConfig defaultAirflowConfig;

  /**
   * Airflow 3 backend config, present only when Airflow 3 is enabled
   * ({@code osdu.airflow.version=airflow3}). Resolved lazily via {@link #getAirflow3Config()}.
   */
  @Autowired
  @Qualifier("airflow3Config")
  private ObjectProvider<AirflowConfig> airflow3ConfigProvider;

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

  /**
   * Returns the Airflow 3 backend config. Airflow 3 uses a single deployment-wide endpoint
   * ({@code osdu.airflow.airflow3.*}) rather than per-partition endpoints.
   *
   * @throws AppException if Airflow 3 routing is requested but the backend is not configured.
   */
  public AirflowConfig getAirflow3Config() {
    AirflowConfig airflow3Config = airflow3ConfigProvider.getIfAvailable();
    if (airflow3Config == null) {
      throw new AppException(
          500,
          "Airflow 3 backend not configured",
          "A run owned by engineVersion=airflow3 was encountered but the airflow3Config bean is "
              + "not registered. Enable it via osdu.airflow.version=airflow3 and set "
              + "osdu.airflow.airflow3.url.");
    }
    return airflow3Config;
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
