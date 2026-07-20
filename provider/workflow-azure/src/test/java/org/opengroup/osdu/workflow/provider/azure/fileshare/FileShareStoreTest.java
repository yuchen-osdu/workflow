package org.opengroup.osdu.workflow.provider.azure.fileshare;

import com.azure.core.http.HttpResponse;
import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.models.ShareStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class FileShareStoreTest {
  private static final String PARTITION_ID = "dataPartitionId";
  private static final String FILE_SHARE_NAME = "fileShareName";
  private static final String DIRECTORY_NAME = "directoryName";
  private static final String FILE_NAME = "fileName";
  private static final String CONTENT = "hello world";

  @Mock
  private CoreLoggerFactory coreLoggerFactory;

  @Mock
  private CoreLogger coreLogger;

  @Mock
  private ILogger logger;

  @Mock
  private ShareFileClient fileShareFileClient;

  @Mock
  private ShareDirectoryClient fileShareDirectoryClient;

  @Mock
  private ShareClient fileShareClient;

  @Mock
  private ShareServiceClient fileShareServiceClient;

  @Mock
  private IFileShareServiceClientFactory fileShareServiceClientFactory;

  @InjectMocks
  private FileShareStore fileShareStore;

  /**
   * Workaround for inability to mock static methods like getInstance().
   *
   * @param mock CoreLoggerFactory mock instance
   */
  private void mockSingleton(CoreLoggerFactory mock) {
    try {
      Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, mock);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reset workaround for inability to mock static methods like getInstance().
   */
  private void resetSingleton() {
    try {
      Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
      instance.setAccessible(false);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void init() {
    initMocks(this);
    mockSingleton(coreLoggerFactory);
    when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

    lenient().when(fileShareDirectoryClient.getFileClient(FILE_NAME)).thenReturn(fileShareFileClient);
    lenient().when(fileShareClient.getDirectoryClient(DIRECTORY_NAME)).thenReturn(fileShareDirectoryClient);
    lenient().when(fileShareServiceClient.getShareClient(FILE_SHARE_NAME)).thenReturn(fileShareClient);
    lenient().when(fileShareServiceClientFactory.getFileShareServiceClient(PARTITION_ID)).thenReturn(fileShareServiceClient);
    lenient().doNothing().when(logger).warning(eq("azure-core-lib"), any(), anyMap());
  }

  @AfterEach
  public void takeDown() {
    resetSingleton();
  }

  @Test
  public void writeToFileShare_Success() {
    when(fileShareDirectoryClient.createFile(eq(FILE_NAME), eq((long) CONTENT.getBytes().length))).thenReturn(fileShareFileClient);

    fileShareStore.writeToFileShare(PARTITION_ID, FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME, CONTENT);

    ArgumentCaptor<ByteArrayInputStream> inputStream = ArgumentCaptor.forClass(ByteArrayInputStream.class);
    // validate that the upload method is being invoked appropriately.
    verify(fileShareFileClient).upload(inputStream.capture(), eq((long) CONTENT.getBytes().length));
  }

  @Test
  public void writeToFileShare_Failure() {
    HttpResponse httpResponse = mock(HttpResponse.class);
    doThrow(new ShareStorageException("message", httpResponse, null)).when(fileShareDirectoryClient).createFile(eq(FILE_NAME), eq((long) CONTENT.getBytes().length));

    Assertions.assertThrows(AppException.class, () -> {
      fileShareStore.writeToFileShare(PARTITION_ID, FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME, CONTENT);
    });
  }

  @Test
  public void writeToFileShareInternal_Success() {
    when(fileShareServiceClientFactory.getSystemFileShareServiceClient()).thenReturn(fileShareServiceClient);
    when(fileShareDirectoryClient.createFile(eq(FILE_NAME), eq((long) CONTENT.getBytes().length))).thenReturn(fileShareFileClient);

    fileShareStore.writeToFileShare(FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME, CONTENT);

    ArgumentCaptor<ByteArrayInputStream> inputStream = ArgumentCaptor.forClass(ByteArrayInputStream.class);
    // validate that the upload method is being invoked appropriately.
    verify(fileShareFileClient).upload(inputStream.capture(), eq((long) CONTENT.getBytes().length));
  }

  @Test
  public void writeToFileShareInternal_Failure() {
    HttpResponse httpResponse = mock(HttpResponse.class);
    doThrow(new ShareStorageException("message", httpResponse, null)).when(fileShareDirectoryClient).createFile(eq(FILE_NAME), eq((long) CONTENT.getBytes().length));
    when(fileShareServiceClientFactory.getSystemFileShareServiceClient()).thenReturn(fileShareServiceClient);

    Assertions.assertThrows(AppException.class, () -> {
        fileShareStore.writeToFileShare(FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME, CONTENT);
    });
  }

  @Test
  public void deleteFromFileShare_Success() {
    fileShareStore.deleteFromFileShare(PARTITION_ID, FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME);
    // validate that the delete method is being invoked appropriately.
    verify(fileShareDirectoryClient).deleteFile(eq(FILE_NAME));
  }

  @Test
  public void deleteFromFileShare_Failure() {
    HttpResponse httpResponse = mock(HttpResponse.class);
    doThrow(new ShareStorageException("message", httpResponse, null)).when(fileShareDirectoryClient).deleteFile(eq(FILE_NAME));

    Assertions.assertThrows(AppException.class, () -> {
      fileShareStore.deleteFromFileShare(PARTITION_ID, FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME);
    });

    // validate that the delete method is being invoked appropriately.
    verify(fileShareDirectoryClient).deleteFile(eq(FILE_NAME));
  }

  @Test
  public void deleteFromFileShareInternal_Success() {
    when(fileShareServiceClientFactory.getSystemFileShareServiceClient()).thenReturn(fileShareServiceClient);
    fileShareStore.deleteFromFileShare(FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME);
    // validate that the delete method is being invoked appropriately.
    verify(fileShareDirectoryClient).deleteFile(eq(FILE_NAME));
  }

  @Test
  public void deleteFromFileShareInternal_Failure() {
    HttpResponse httpResponse = mock(HttpResponse.class);
    when(fileShareServiceClientFactory.getSystemFileShareServiceClient()).thenReturn(fileShareServiceClient);
    doThrow(new ShareStorageException("message", httpResponse, null)).when(fileShareDirectoryClient).deleteFile(eq(FILE_NAME));

    Assertions.assertThrows(AppException.class, () -> {
      fileShareStore.deleteFromFileShare(FILE_SHARE_NAME, DIRECTORY_NAME, FILE_NAME);
    });

    // validate that the delete method is being invoked appropriately.
    verify(fileShareDirectoryClient).deleteFile(eq(FILE_NAME));
  }
}
