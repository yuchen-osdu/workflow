package org.opengroup.osdu.workflow.provider.azure.fileshare;

import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.workflow.provider.azure.cache.FileShareServiceClientCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileShareStoreProvider {

  @Bean("IngestFileShareServiceClientFactory")
  public IFileShareServiceClientFactory buildFileShareClientFactory(final PartitionServiceClient partitionServiceClient,
                                                                    final FileShareServiceClientCache fileShareServiceClientCache) {
    return new DAGFileShareServiceClientFactoryImpl(partitionServiceClient, fileShareServiceClientCache);
  }

  @Bean
  @Qualifier("IngestFileShareStore")
  public FileShareStore buildFileShareStore(@Qualifier("IngestFileShareServiceClientFactory") final IFileShareServiceClientFactory fileShareServiceClientFactory, final ILogger logger) {
    return new FileShareStore(fileShareServiceClientFactory, logger);
  }
}
