package org.opengroup.osdu.workflow.provider.azure.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.workflow.provider.azure.interfaces.IActiveDagRunsCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.opengroup.osdu.workflow.provider.azure.consts.CacheConstants.ACTIVE_DAG_RUNS_LOCAL_CACHE_EXPIRATION_SECONDS;
import static org.opengroup.osdu.workflow.provider.azure.consts.CacheConstants.ACTIVE_DAG_RUNS_LOCAL_CACHE_MAXIMUM_SIZE;

@Component("ActiveDagRunsCache")
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "true")
public class ActiveDagRunsVmCache extends VmCache<String, Integer> implements IActiveDagRunsCache<String, Integer> {
  public ActiveDagRunsVmCache() {
    super(ACTIVE_DAG_RUNS_LOCAL_CACHE_EXPIRATION_SECONDS, ACTIVE_DAG_RUNS_LOCAL_CACHE_MAXIMUM_SIZE);
  }

  @Override
  public void incrementKey(String key) {
    Integer value = this.get(key);
    this.put(key, value + 1);
  }

  @Override
  public void decrementKey(String key) {
    Integer value = this.get(key);
    this.put(key, value - 1);
  }
}
