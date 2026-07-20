package org.opengroup.osdu.workflow.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
public class ClientResponse {
  private HttpStatus status;
  private Object responseBody;
  private final String contentEncoding;
  private final String contentType;
  private final int statusCode;
  private final String statusMessage;
}
