/*
 *  Copyright 2020-2025 Google LLC
 *  Copyright 2020-2025 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.service;

import static java.lang.String.format;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicAuthAirflowApiClient implements IAirflowApiClient {
  private final Client restClient;
  private final AirflowConfig airflowConfig;

  @Override
  public ClientResponse callAirflow(
      String httpMethod,
      String apiEndpoint,
      String body,
      WorkflowEngineRequest rq,
      String errorMessage) {
    String url = format("%s/%s", airflowConfig.getUrl(), apiEndpoint);
    log.info("Calling airflow endpoint {} with method {}", url, httpMethod);

    WebResource webResource = restClient.resource(url);
    com.sun.jersey.api.client.ClientResponse response;
    try {
      response =
          webResource
              .type(MediaType.APPLICATION_JSON)
              .header("Authorization", "Basic " + airflowConfig.getAppKey())
              .method(httpMethod, com.sun.jersey.api.client.ClientResponse.class, body);
    } catch (UniformInterfaceException | ClientHandlerException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Error calling airflow",
          "Error calling airflow: %s".formatted(e.getMessage()),
          e);
    }

    final int status = response.getStatus();
    log.info("Received response status: {}.", status);

    if (status != HttpStatus.OK.value()) {
      String responseBody = response.getEntity(String.class);
      throw new AppException(status, responseBody, errorMessage);
    }

    return ClientResponse.builder()
        .contentType(String.valueOf(response.getType()))
        .responseBody(response.getEntity(String.class))
        .status(HttpStatus.OK)
        .statusCode(response.getStatus())
        .statusMessage(response.getStatusInfo().getReasonPhrase())
        .build();
  }
}
