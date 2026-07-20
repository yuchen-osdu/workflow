package org.opengroup.osdu.workflow.exception;

import org.opengroup.osdu.core.common.exception.CoreException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceConflictException extends CoreException {
  private String conflictingResourceId;

  public ResourceConflictException(String conflictingResourceId, String message) {
    super(message);
    this.conflictingResourceId = conflictingResourceId;
  }

  public ResourceConflictException(String conflictingResourceId, String message, Throwable cause) {
    super(message, cause);
    this.conflictingResourceId = conflictingResourceId;
  }

  public String getConflictingResourceId() {
    return conflictingResourceId;
  }
}
