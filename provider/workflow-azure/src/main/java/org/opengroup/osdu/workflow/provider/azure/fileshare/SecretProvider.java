package org.opengroup.osdu.workflow.provider.azure.fileshare;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecretProvider {
  @Autowired
  private SecretClient secretClient;

  public String getKeyVaultSecret(final String secretName) {
    KeyVaultSecret secret = secretClient.getSecret(secretName);
    if (secret == null) {
      throw new IllegalStateException(String.format("No secret found with name %s", secretName));
    }

    String secretValue = secret.getValue();
    if (secretValue == null) {
      throw new IllegalStateException(String.format(
          "Secret unexpectedly missing from KeyVault response for secret with name %s", secretName));
    }

    return secretValue;
  }
}
