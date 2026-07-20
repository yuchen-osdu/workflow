package org.opengroup.osdu.workflow.provider.azure.cache;

import com.azure.storage.blob.BlobServiceClient;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class IngestBlobServiceClientCache extends VmCache<String, BlobServiceClient> {

  public IngestBlobServiceClientCache() {
    super(60 * 60, 1000);
  }

  public boolean containsKey(final String key) {
    return this.get(key) != null;
  }
}
