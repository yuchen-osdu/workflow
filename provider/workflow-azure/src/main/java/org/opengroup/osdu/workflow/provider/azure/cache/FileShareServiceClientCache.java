package org.opengroup.osdu.workflow.provider.azure.cache;

import com.azure.storage.file.share.ShareServiceClient;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of ICache for ShareServiceClient.
 */
@Component
@Lazy
public class FileShareServiceClientCache extends VmCache<String, ShareServiceClient> {
  /**
   *  Default cache constructor.
   */
  public FileShareServiceClientCache() {
    super(60 * 60, 1000);
  }

  /**
   * @param key cache key
   * @return true if found in cache
   */
  public boolean containsKey(final String key) {
    return this.get(key) != null;
  }
}
