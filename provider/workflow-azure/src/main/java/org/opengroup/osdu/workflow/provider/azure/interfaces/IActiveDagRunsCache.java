package org.opengroup.osdu.workflow.provider.azure.interfaces;

import org.opengroup.osdu.core.common.cache.ICache;

public interface IActiveDagRunsCache<K, V> extends ICache<K, V> {
  void incrementKey(K key);
  void decrementKey(K key);
}
