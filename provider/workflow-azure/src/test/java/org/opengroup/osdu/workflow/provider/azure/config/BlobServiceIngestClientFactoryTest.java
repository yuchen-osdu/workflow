package org.opengroup.osdu.workflow.provider.azure.config;

import com.azure.cosmos.implementation.apachecommons.lang.NotImplementedException;
import com.azure.identity.DefaultAzureCredential;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.workflow.provider.azure.cache.IngestBlobServiceClientCache;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BlobServiceIngestClientFactoryTest {

  private static final String PARTITION_ID = "dataPartitionId";
  private static final String STORAGE_ACCOUNT_NAME = "storage-account";

  @Mock
  private DefaultAzureCredential defaultAzureCredential;

  @Mock
  private PartitionServiceClient partitionService;

  @Mock
  private IngestBlobServiceClientCache clientCache;

  @InjectMocks
  private BlobServiceIngestClientFactory blobServiceIngestClientFactory;

  @Test
  public void testGetBlobServiceClient_whenServiceClientPresentInCache() {
    BlobServiceClient blobServiceClientMock = mock(BlobServiceClient.class);
    String cacheKey = String.format("%s-ingest-blobServiceClient", PARTITION_ID);
    when(clientCache.containsKey(cacheKey)).thenReturn(true);
    when(clientCache.get(cacheKey)).thenReturn(blobServiceClientMock);

    BlobServiceClient blobServiceClient = blobServiceIngestClientFactory.getBlobServiceClient(PARTITION_ID);

    assertEquals(blobServiceClient, blobServiceClientMock);
  }

  @Test
  public void testGetBlobServiceClient_whenServiceClientNotPresentInCache() {
    PartitionInfoAzure pi = mock(PartitionInfoAzure.class);
    String cacheKey = String.format("%s-ingest-blobServiceClient", PARTITION_ID);
    when(clientCache.containsKey(cacheKey)).thenReturn(false);
    when(partitionService.getPartition(eq(PARTITION_ID))).thenReturn(pi);
    when(pi.getIngestStorageAccountName()).thenReturn(STORAGE_ACCOUNT_NAME);
    BlobServiceClient blobServiceClient = blobServiceIngestClientFactory.getBlobServiceClient(PARTITION_ID);

    verify(clientCache, times(0)).get(eq(cacheKey));
    verify(clientCache, times(1)).put(eq(cacheKey), eq(blobServiceClient));
  }

  @Test
  public void testGetSystemBlobServiceClient() {
    Assertions.assertThrows(NotImplementedException.class, () -> {
      blobServiceIngestClientFactory.getSystemBlobServiceClient();
    });
  }
}
