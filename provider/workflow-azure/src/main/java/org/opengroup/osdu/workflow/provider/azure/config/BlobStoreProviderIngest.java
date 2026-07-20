package org.opengroup.osdu.workflow.provider.azure.config;

import com.azure.identity.DefaultAzureCredential;
import org.opengroup.osdu.azure.blobstorage.BlobStore;
import org.opengroup.osdu.azure.blobstorage.IBlobServiceClientFactory;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.workflow.provider.azure.cache.IngestBlobServiceClientCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStoreProviderIngest {

  @Autowired
  DefaultAzureCredential defaultAzureCredential;

  @Autowired
  PartitionServiceClient partitionService;

  @Autowired
  IngestBlobServiceClientCache clientCache;

  @Bean
  @Qualifier("IngestBlobServiceClientFactory")
  public IBlobServiceClientFactory buildBlobClientFactory(final DefaultAzureCredential defaultAzureCredential,
                                                          final PartitionServiceClient partitionServiceClient,
                                                          final IngestBlobServiceClientCache blobServiceClientCache) {
    return new BlobServiceIngestClientFactory(defaultAzureCredential, partitionServiceClient, blobServiceClientCache);
  }

  @Bean
  @Qualifier("IngestBlobStore")
  public BlobStore buildBlobStore(@Qualifier("IngestBlobServiceClientFactory") final IBlobServiceClientFactory blobServiceClientFactory, final ILogger logger, DependencyLogger depLogger) {
    return new BlobStore(blobServiceClientFactory, logger, depLogger);
  }
}
