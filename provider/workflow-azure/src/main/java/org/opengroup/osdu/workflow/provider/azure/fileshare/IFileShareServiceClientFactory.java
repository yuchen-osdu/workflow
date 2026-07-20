package org.opengroup.osdu.workflow.provider.azure.fileshare;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.file.share.ShareServiceClient;

/**
 *  Interface for File share service client factory to return appropriate
 *  share service client based on the data partition id.
 */
public interface IFileShareServiceClientFactory {
  /**
   *
   * @param   dataPartitionId data partition id.
   * @return  ShareServiceClient corresponding to the given data partition id.
   */
  ShareServiceClient getFileShareServiceClient(String dataPartitionId);

  /**
   *
   * @return      BlobServiceClient for system resources.
   */
  ShareServiceClient getSystemFileShareServiceClient();
}
