package org.opengroup.osdu.workflow.provider.azure.exception;

import org.opengroup.osdu.core.common.exception.CoreException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CustomOperatorNotFoundException extends CoreException {
  public CustomOperatorNotFoundException(String message) {
    super(message);
  }
}
