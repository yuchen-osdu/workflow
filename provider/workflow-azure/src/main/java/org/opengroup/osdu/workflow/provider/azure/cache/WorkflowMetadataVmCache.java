package org.opengroup.osdu.workflow.provider.azure.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.workflow.model.WorkflowMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.opengroup.osdu.workflow.provider.azure.consts.CacheConstants.WORKFLOW_METADATA_LOCAL_CACHE_EXPIRATION_SECONDS;
import static org.opengroup.osdu.workflow.provider.azure.consts.CacheConstants.WORKFLOW_METADATA_LOCAL_CACHE_MAXIMUM_SIZE;

@Component("WorkflowMetadataCache")
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "true")
public class WorkflowMetadataVmCache extends VmCache<String, WorkflowMetadata> {
  public WorkflowMetadataVmCache() {
    super(WORKFLOW_METADATA_LOCAL_CACHE_EXPIRATION_SECONDS, WORKFLOW_METADATA_LOCAL_CACHE_MAXIMUM_SIZE);
  }
}
