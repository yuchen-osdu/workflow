package org.opengroup.osdu.workflow.provider.azure.fileshare;

import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.models.ShareStorageException;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.time.Duration;

/**
 * An interface to interact with Azure file share.
 */
public class FileShareStore {
  private static final String LOGGER_NAME = FileShareStore.class.getName();
  private ILogger logger;

  @Autowired
  private IFileShareServiceClientFactory fileShareClientFactory;

  /**
   * Constructor to create FileShareStore.
   *
   * @param factory        Factory that provides fileshare client.
   * @param loggerInstance logger instance to be used for logging.
   */
  public FileShareStore(final IFileShareServiceClientFactory factory, final ILogger loggerInstance) {
    this.fileShareClientFactory = factory;
    this.logger = loggerInstance;
  }

  /**
   * writeToFileShare: Writes the given content in the given file in file share.
   * @param dataPartitionId Data partition id
   * @param fileShareName   Name of the file share
   * @param directoryName   Name of the directory
   * @param fileName        Name of the file
   * @param content        Content to be written in the file
   */
  public void writeToFileShare(final String dataPartitionId,
                               final String fileShareName,
                               final String directoryName,
                               final String fileName,
                               final String content) {
    ShareDirectoryClient directoryClient = getShareDirectoryClient(dataPartitionId, fileShareName, directoryName);
    writeToFileShareInternal(directoryClient, dataPartitionId, fileShareName, directoryName,
        fileName, content);
  }

  /**
   * writeToFileShare: Writes the given content in the given file in system file share.
   * @param fileShareName   Name of the file share
   * @param directoryName   Name of the directory
   * @param fileName        Name of the file
   * @param content        Content to be written in the file
   */
  public void writeToFileShare(final String fileShareName,
                               final String directoryName,
                               final String fileName,
                               final String content) {
    ShareDirectoryClient directoryClient = getSystemShareDirectoryClient(fileShareName, directoryName);
    writeToFileShareInternal(directoryClient, "system", fileShareName, directoryName,
        fileName, content);
  }

  private void writeToFileShareInternal(ShareDirectoryClient directoryClient,
                                        final String dataPartitionId, final String fileShareName,
                                        final String directoryName, final String fileName,
                                        final String content) {
    final long start = System.currentTimeMillis();
    int statusCode = HttpStatus.SC_OK;
    try {
      directoryClient.createFile(fileName, content.getBytes().length).upload(new ByteArrayInputStream(content.getBytes()), content.getBytes().length);
      CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("{}", MessageFormatter.format("Done creating file {}/{}", directoryName, fileName).getMessage());
    } catch (ShareStorageException ex) {
      statusCode = ex.getStatusCode();
      throw handleShareStorageException(500, "Failed to create file.", ex);
    } finally {
      final long timeTaken = System.currentTimeMillis() - start;
      final String dependencyData = MessageFormatter.arrayFormat("{}:{}/{}/{}", new String[]{dataPartitionId, fileShareName, directoryName, fileName}).getMessage();
      logDependency("WRITE_TO_FILE_SHARE", dependencyData, dependencyData, timeTaken, String.valueOf(statusCode), statusCode == HttpStatus.SC_OK);
    }
  }

  /**
   * deleteFromFileShare: Deletes given file from the file share.
   * @param dataPartitionId Data partition id
   * @param fileShareName   Name of the file share
   * @param directoryName   Name of the directory
   * @param fileName        Name of the file
   */
  public void deleteFromFileShare(final String dataPartitionId,
                                  final String fileShareName,
                                  final String directoryName,
                                  final String fileName) {
    ShareDirectoryClient directoryClient = getShareDirectoryClient(dataPartitionId,
        fileShareName, directoryName);
    deleteFromFileShareInternal(directoryClient, dataPartitionId, fileShareName,
        directoryName, fileName);
  }

  /**
   * deleteFromFileShare: Deletes given file from the system file share.
   * @param fileShareName   Name of the file share
   * @param directoryName   Name of the directory
   * @param fileName        Name of the file
   */
  public void deleteFromFileShare(final String fileShareName, final String directoryName,
                                  final String fileName) {
    ShareDirectoryClient directoryClient = getSystemShareDirectoryClient(
        fileShareName, directoryName);
    deleteFromFileShareInternal(directoryClient, "system", fileShareName,
        directoryName, fileName);
  }

  private void deleteFromFileShareInternal(ShareDirectoryClient directoryClient,
                                           final String dataPartitionId, final String fileShareName,
                                           final String directoryName, final String fileName) {
    final long start = System.currentTimeMillis();
    int statusCode = HttpStatus.SC_OK;
    try {
      directoryClient.deleteFile(fileName);
      CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("{}", MessageFormatter.format("Done deleting file {}/{}", directoryName, fileName).getMessage());
    } catch (ShareStorageException ex) {
      statusCode = ex.getStatusCode();
      throw handleShareStorageException(500, "Failed to delete file.", ex);
    } finally {
      final long timeTaken = System.currentTimeMillis() - start;
      final String dependencyData = MessageFormatter.arrayFormat("{}:{}/{}/{}", new String[]{dataPartitionId, fileShareName, directoryName, fileName}).getMessage();
      logDependency("DELETE_FROM_FILE_SHARE", dependencyData, dependencyData, timeTaken, String.valueOf(statusCode), statusCode == HttpStatus.SC_OK);
    }
  }

  /**
   * Logs and returns instance of AppException.
   *
   * @param status       Response status code
   * @param errorMessage Error message
   * @param ex           Original exception
   * @return Instance of AppException
   */
  private AppException handleShareStorageException(final int status, final String errorMessage, final Exception ex) {
    CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(MessageFormatter.format("{}", errorMessage).getMessage(), ex);
    return new AppException(status, errorMessage, ex.getMessage(), ex);
  }

  /**
   * Log dependency.
   *
   * @param name          the name of the command initiated with this dependency call
   * @param data          the command initiated by this dependency call
   * @param target        the target of this dependency call
   * @param timeTakenInMs the request duration in milliseconds
   * @param resultCode    the result code of the call
   * @param success       indication of successful or unsuccessful call
   */
  private void logDependency(final String name, final String data, final String target, final long timeTakenInMs, final String resultCode, final boolean success) {
    DependencyPayload payload = new DependencyPayload(name, data, Duration.ofMillis(timeTakenInMs), resultCode, success);
    payload.setType("FileShare");
    payload.setTarget(target);
    CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).logDependency(payload);
  }

  /**
   * @param dataPartitionId Data partition id.
   * @param fileShareName   Name of the file share
   * @param directoryName   Name of the directory
   * @return share directory client.
   */
  private ShareDirectoryClient getShareDirectoryClient(final String dataPartitionId, final String fileShareName, final String directoryName) {
    ShareClient shareClient = getShareClient(dataPartitionId, fileShareName);
    return shareClient.getDirectoryClient(directoryName);
  }

  /**
   * @param dataPartitionId Data partition id.
   * @param fileShareName   Name of the file share
   * @return share client.
   */
  private ShareClient getShareClient(final String dataPartitionId, final String fileShareName) {
    ShareServiceClient shareServiceClient = fileShareClientFactory.getFileShareServiceClient(dataPartitionId);
    return shareServiceClient.getShareClient(fileShareName);
  }

  /**
   * @param fileShareName   Name of the file share
   * @param directoryName   Name of the directory
   * @return System share directory client.
   */
  private ShareDirectoryClient getSystemShareDirectoryClient(final String fileShareName, final String directoryName) {
    ShareClient shareClient = getSystemShareClient(fileShareName);
    return shareClient.getDirectoryClient(directoryName);
  }

  /**
   * @param fileShareName   Name of the file share
   * @return System share client.
   */
  private ShareClient getSystemShareClient(final String fileShareName) {
    ShareServiceClient shareServiceClient =
        fileShareClientFactory.getSystemFileShareServiceClient();
    return shareServiceClient.getShareClient(fileShareName);
  }
}
