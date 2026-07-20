package org.opengroup.osdu.workflow.provider.azure.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class AirflowConfigCache extends VmCache<String, AirflowConfig> {
  public AirflowConfigCache() {
    super(60 * 60, 1000);
  }

  public boolean containsKey(final String key) {
    return this.get(key) != null;
  }
}
