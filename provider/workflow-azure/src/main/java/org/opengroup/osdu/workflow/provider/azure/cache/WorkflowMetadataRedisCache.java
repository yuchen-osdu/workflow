package org.opengroup.osdu.workflow.provider.azure.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.opengroup.osdu.workflow.provider.azure.config.RedisConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("WorkflowMetadataCache")
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "false", matchIfMissing = true)
public class WorkflowMetadataRedisCache extends RedisCache<String, WorkflowMetadata> {
  public WorkflowMetadataRedisCache(final RedisConfig redisConfig) {
    super(redisConfig.getRedisHost(), redisConfig.getRedisPort(), redisConfig.getRedisPassword(), redisConfig.getWorkflowMetadataTtl(), String.class, WorkflowMetadata.class);
  }
}
