package org.opengroup.osdu.workflow.provider.azure.fileshare;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecretProviderTest {

  private static final String SECRET_NAME = "secret-name";
  private static final String SECRET_VALUE = "secret-value";

  @Mock
  private SecretClient secretClient;

  @InjectMocks
  private SecretProvider secretProvider;

  @Test
  public void test_getSecretFromKeyVaultSuccess() {
    KeyVaultSecret keyVaultSecretMock = mock(KeyVaultSecret.class);
    when(secretClient.getSecret(eq(SECRET_NAME))).thenReturn(keyVaultSecretMock);
    when(keyVaultSecretMock.getValue()).thenReturn(SECRET_VALUE);

    String secretValueObtained = secretProvider.getKeyVaultSecret(SECRET_NAME);

    assertEquals(SECRET_VALUE, secretValueObtained);
  }

  @Test
  public void test_getSecretFromKeyVaultFailure_noSecretFound() {
    when(secretClient.getSecret(eq(SECRET_NAME))).thenReturn(null);

    Assertions.assertThrows(IllegalStateException.class, () -> {
      secretProvider.getKeyVaultSecret(SECRET_NAME);
    });
  }

  @Test
  public void test_getSecretFromKeyVaultFailure_noSecretValue() {
    KeyVaultSecret keyVaultSecretMock = mock(KeyVaultSecret.class);
    when(secretClient.getSecret(eq(SECRET_NAME))).thenReturn(keyVaultSecretMock);
    when(keyVaultSecretMock.getValue()).thenReturn(null);

    Assertions.assertThrows(IllegalStateException.class, () -> {
      secretProvider.getKeyVaultSecret(SECRET_NAME);
    });
  }
}
