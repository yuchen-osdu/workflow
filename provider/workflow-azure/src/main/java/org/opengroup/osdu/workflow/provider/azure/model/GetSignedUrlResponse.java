package org.opengroup.osdu.workflow.provider.azure.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetSignedUrlResponse {
  private final String url;
}
