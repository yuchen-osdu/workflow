package org.opengroup.osdu.workflow.provider.azure.config;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.azure.cache.AirflowConfigCache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AirflowConfigResolverTest {
  private static final String DATA_PARTITION_ID = "test";
  private static final String CACHE_KEY = "test-airflow-config";
  private static final String DEFAULT_AIRFLOW_URL = "http://airflow";
  private static final String DEFAULT_AIRFLOW_USERNAME = "airflow";
  private static final String DEFAULT_AIRFLOW_PASSWORD = "password";
  private static final String DP_AIRFLOW_URL = "http://dp-airflow";
  private static final String DP_AIRFLOW_USERNAME = "dp-airflow";
  private static final String DP_AIRFLOW_PASSWORD = "dp-password";

  @Mock
  private PartitionServiceClient partitionServiceClient;

  @Spy
  private AirflowConfigCache airflowConfigCache = new AirflowConfigCache();

  @Mock
  private AirflowConfig defaultAirflowConfig;

  @InjectMocks
  private AirflowConfigResolver airflowConfigResolver;

  @Test
  public void testConfigResolverReturnsDefaultIfAirflowDisabled() {
    when(partitionServiceClient.getPartition(DATA_PARTITION_ID)).thenReturn(
        PartitionInfoAzure.builder()
            .airflowEnabledConfig(Property.builder().sensitive(false).value("false").build())
            .build());

    AirflowConfig airflowConfig = airflowConfigResolver.getAirflowConfig(DATA_PARTITION_ID);

    verify(airflowConfigCache, times(1)).containsKey(CACHE_KEY);
    verify(partitionServiceClient).getPartition(DATA_PARTITION_ID);
    verify(airflowConfigCache).put(CACHE_KEY, defaultAirflowConfig);
    Assert.assertEquals(defaultAirflowConfig, airflowConfig);
  }

  @Test
  public void testConfigResolverReturnsDefaultFromCacheIfDisabledAndMultipleCalls() {
    when(partitionServiceClient.getPartition(DATA_PARTITION_ID)).thenReturn(
        PartitionInfoAzure.builder()
            .airflowEnabledConfig(Property.builder().sensitive(false).value("false").build())
            .build());
    when(defaultAirflowConfig.getUrl()).thenReturn(DEFAULT_AIRFLOW_URL);
    when(defaultAirflowConfig.getUsername()).thenReturn(DEFAULT_AIRFLOW_USERNAME);
    when(defaultAirflowConfig.getPassword()).thenReturn(DEFAULT_AIRFLOW_PASSWORD);

    AirflowConfig airflowConfig = airflowConfigResolver.getAirflowConfig(DATA_PARTITION_ID);

    verify(airflowConfigCache).containsKey(CACHE_KEY);
    verify(partitionServiceClient).getPartition(DATA_PARTITION_ID);
    verify(airflowConfigCache).put(CACHE_KEY, defaultAirflowConfig);
    Assert.assertEquals(DEFAULT_AIRFLOW_URL, airflowConfig.getUrl());
    Assert.assertEquals(DEFAULT_AIRFLOW_USERNAME, airflowConfig.getUsername());
    Assert.assertEquals(DEFAULT_AIRFLOW_PASSWORD, airflowConfig.getPassword());

    airflowConfig = airflowConfigResolver.getAirflowConfig(DATA_PARTITION_ID);

    verify(airflowConfigCache, times(2)).containsKey(CACHE_KEY);
    verify(airflowConfigCache, times(3)).get(CACHE_KEY);
    verify(partitionServiceClient, times(1)).getPartition(DATA_PARTITION_ID);
    verify(airflowConfigCache, times(1)).put(CACHE_KEY, defaultAirflowConfig);
    Assert.assertEquals(DEFAULT_AIRFLOW_URL, airflowConfig.getUrl());
    Assert.assertEquals(DEFAULT_AIRFLOW_USERNAME, airflowConfig.getUsername());
    Assert.assertEquals(DEFAULT_AIRFLOW_PASSWORD, airflowConfig.getPassword());
  }

  @Test
  public void testConfigResolverReturnsDPConfigIfAirflowEnabled() {
    when(partitionServiceClient.getPartition(DATA_PARTITION_ID)).thenReturn(
        PartitionInfoAzure.builder()
            .airflowEnabledConfig(Property.builder().sensitive(false).value("true").build())
            .airflowEndpointConfig(Property.builder().sensitive(false).value(DP_AIRFLOW_URL).build())
            .airflowUsernameConfig(Property.builder().sensitive(false).value(DP_AIRFLOW_USERNAME).build())
            .airflowPasswordConfig(Property.builder().sensitive(false).value(DP_AIRFLOW_PASSWORD).build())
            .build());

    AirflowConfig airflowConfig = airflowConfigResolver.getAirflowConfig(DATA_PARTITION_ID);

    verify(airflowConfigCache).containsKey(CACHE_KEY);
    verify(partitionServiceClient).getPartition(DATA_PARTITION_ID);
    Assert.assertEquals(DP_AIRFLOW_URL, airflowConfig.getUrl());
    Assert.assertEquals(DP_AIRFLOW_USERNAME, airflowConfig.getUsername());
    Assert.assertEquals(DP_AIRFLOW_PASSWORD, airflowConfig.getPassword());
  }

  @Test
  public void testConfigResolverReturnsDPConfigFromCacheIfEnabledAndMultipleCalls() {
    when(partitionServiceClient.getPartition(DATA_PARTITION_ID)).thenReturn(
        PartitionInfoAzure.builder()
            .airflowEnabledConfig(Property.builder().sensitive(false).value("true").build())
            .airflowEndpointConfig(Property.builder().sensitive(false).value(DP_AIRFLOW_URL).build())
            .airflowUsernameConfig(Property.builder().sensitive(false).value(DP_AIRFLOW_USERNAME).build())
            .airflowPasswordConfig(Property.builder().sensitive(false).value(DP_AIRFLOW_PASSWORD).build())
            .build());

    AirflowConfig airflowConfig = airflowConfigResolver.getAirflowConfig(DATA_PARTITION_ID);

    verify(airflowConfigCache).containsKey(CACHE_KEY);
    verify(partitionServiceClient).getPartition(DATA_PARTITION_ID);
    Assert.assertEquals(DP_AIRFLOW_URL, airflowConfig.getUrl());
    Assert.assertEquals(DP_AIRFLOW_USERNAME, airflowConfig.getUsername());
    Assert.assertEquals(DP_AIRFLOW_PASSWORD, airflowConfig.getPassword());

    airflowConfig = airflowConfigResolver.getAirflowConfig(DATA_PARTITION_ID);

    verify(airflowConfigCache, times(2)).containsKey(CACHE_KEY);
    verify(airflowConfigCache, times(3)).get(CACHE_KEY);
    verify(partitionServiceClient, times(1)).getPartition(DATA_PARTITION_ID);
    verify(airflowConfigCache, times(1)).put(eq(CACHE_KEY), any());
    Assert.assertEquals(DP_AIRFLOW_URL, airflowConfig.getUrl());
    Assert.assertEquals(DP_AIRFLOW_USERNAME, airflowConfig.getUsername());
    Assert.assertEquals(DP_AIRFLOW_PASSWORD, airflowConfig.getPassword());
  }

  @Test
  public void testConfigResolverReturnsDefaultConfigForSystem() {
    AirflowConfig airflowConfig = airflowConfigResolver.getSystemAirflowConfig();
    Assert.assertEquals(defaultAirflowConfig, airflowConfig);
  }
}
