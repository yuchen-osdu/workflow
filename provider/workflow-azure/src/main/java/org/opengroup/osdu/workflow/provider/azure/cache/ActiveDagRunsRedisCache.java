package org.opengroup.osdu.workflow.provider.azure.cache;

import io.lettuce.core.codec.RedisCodec;
import org.opengroup.osdu.core.common.cache.JsonCodec;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.workflow.provider.azure.config.RedisConfig;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IActiveDagRunsCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("ActiveDagRunsCache")
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "false", matchIfMissing = true)
public class ActiveDagRunsRedisCache extends RedisCache<String, Integer> implements IActiveDagRunsCache<String, Integer> {

  public ActiveDagRunsRedisCache(final RedisConfig redisConfig) {
    super(redisConfig.getRedisHost(), redisConfig.getRedisPort(), redisConfig.getRedisPassword(), redisConfig.getActiveDagRunsTtl(), String.class, Integer.class);
  }

  @Override
  public void incrementKey(String key) {
    this.increment(key);
  }

  @Override
  public void decrementKey(String key) {
    this.decrement(key);
  }

  @Override
  public RedisCodec<String, Integer> getCodec(Class<String> classOfK, Class<Integer> classOfV) {
    return new JsonCodec<>(classOfK, classOfV);
  }
}
