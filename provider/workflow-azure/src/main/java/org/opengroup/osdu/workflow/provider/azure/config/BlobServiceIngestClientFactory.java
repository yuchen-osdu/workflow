package org.opengroup.osdu.workflow.provider.azure.config;

import com.azure.cosmos.implementation.apachecommons.lang.NotImplementedException;
import com.azure.identity.DefaultAzureCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.opengroup.osdu.azure.blobstorage.IBlobServiceClientFactory;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.workflow.provider.azure.cache.IngestBlobServiceClientCache;


class BlobServiceIngestClientFactory implements IBlobServiceClientFactory {

  private DefaultAzureCredential defaultAzureCredential;

  private PartitionServiceClient partitionService;

  private IngestBlobServiceClientCache clientCache;

  public BlobServiceIngestClientFactory(final DefaultAzureCredential credentials,
                                        final PartitionServiceClient partitionServiceClient,
                                        final IngestBlobServiceClientCache blobServiceClientCache) {
    this.defaultAzureCredential = credentials;
    this.partitionService = partitionServiceClient;
    this.clientCache = blobServiceClientCache;
  }

  @Override
  public BlobServiceClient getBlobServiceClient(final String dataPartitionId) {
    Validators.checkNotNull(defaultAzureCredential, "Credential");
    Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");

    String cacheKey = String.format("%s-ingest-blobServiceClient", dataPartitionId);
    if (this.clientCache.containsKey(cacheKey)) {
      return this.clientCache.get(cacheKey);
    }

    PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);
    String endpoint = String.format("https://%s.blob.core.windows.net", pi.getIngestStorageAccountName());

    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
        .endpoint(endpoint)
        .credential(defaultAzureCredential)
        .buildClient();

    this.clientCache.put(cacheKey, blobServiceClient);

    return blobServiceClient;
  }

  @Override
  public BlobServiceClient getSystemBlobServiceClient() {
    throw new NotImplementedException("getSystemBlobServiceClient method is not implemented");
  }
}
