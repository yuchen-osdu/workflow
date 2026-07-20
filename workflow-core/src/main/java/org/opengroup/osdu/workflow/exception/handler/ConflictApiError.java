package org.opengroup.osdu.workflow.exception.handler;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ConflictApiError extends ApiError {
  private String conflictId;

  @Builder(builderMethodName = "ConflictErrorBuilder")
  public ConflictApiError(String message, List<String> errors,
                          String conflictId) {
    super(HttpStatus.CONFLICT, message, errors);
    this.conflictId = conflictId;
  }
}
