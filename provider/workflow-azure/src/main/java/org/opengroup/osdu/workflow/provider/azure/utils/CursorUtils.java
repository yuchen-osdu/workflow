package org.opengroup.osdu.workflow.provider.azure.utils;

import org.opengroup.osdu.core.common.exception.CoreException;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class CursorUtils {
  public String encodeCosmosCursor(String cosmosCursor) {
    if(cosmosCursor == null) {
      throw new CoreException("Cosmos cursor cannot be null");
    }
    return Base64.getUrlEncoder().encodeToString(cosmosCursor.getBytes());
  }

  public String decodeCosmosCursor(String cursor) {
    if(cursor == null) {
      throw new CoreException("Cursor cannot be null");
    }
    return new String(Base64.getUrlDecoder().decode(cursor));
  }
}
