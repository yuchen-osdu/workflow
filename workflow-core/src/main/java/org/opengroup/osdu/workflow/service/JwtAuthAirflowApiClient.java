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
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;

/**
 * Airflow 3 API client that authenticates against the native FastAPI {@code api/v2} auth manager
 * (SimpleAuthManager / FabAuthManager) using JWT bearer tokens.
 *
 * <p>Unlike Airflow 2's {@code api/v1}, Airflow 3's {@code api/v2} does not accept HTTP Basic
 * credentials. The bearer token is obtained and cached by {@link Airflow3TokenClient} (shared
 * across providers). If the API-server rejects a call with 401/403 (revoked token / rotated signing
 * key), the token is refreshed (herd-safe) and the call is retried once. Any {@code 2xx} response
 * is treated as success (Airflow 3 may answer triggers with 200 or 201).
 */
@Slf4j
public class JwtAuthAirflowApiClient implements IAirflowApiClient {

  private final Client restClient;
  private final AirflowConfig airflowConfig;
  private final Airflow3TokenClient tokenClient;

  public JwtAuthAirflowApiClient(Client restClient, AirflowConfig airflowConfig) {
    this.restClient = restClient;
    this.airflowConfig = airflowConfig;
    this.tokenClient = new Airflow3TokenClient(restClient, airflowConfig);
  }

  @Override
  public ClientResponse callAirflow(
      String httpMethod,
      String apiEndpoint,
      String body,
      WorkflowEngineRequest rq,
      String errorMessage) {
    String url = format("%s/%s", airflowConfig.getUrl(), apiEndpoint);
    log.info("Calling airflow endpoint {} with method {}", url, httpMethod);

    String bearerToken = tokenClient.token();
    com.sun.jersey.api.client.ClientResponse response = invoke(url, httpMethod, body, bearerToken);

    // A 401/403 means the cached token was rejected (rotated key / revoked); refresh (herd-safe)
    // and retry exactly once so transient token issues do not surface as workflow failures.
    if (isUnauthorized(response.getStatus())) {
      log.warn(
          "Airflow returned {} for {}; refreshing JWT and retrying once.",
          response.getStatus(),
          url);
      response.close();
      response = invoke(url, httpMethod, body, tokenClient.refreshIfStale(bearerToken));
    }

    final int status = response.getStatus();
    log.info("Received response status: {}.", status);

    if (!isSuccess(status)) {
      String responseBody = response.getEntity(String.class);
      throw new AppException(status, responseBody, errorMessage);
    }

    return ClientResponse.builder()
        .contentType(String.valueOf(response.getType()))
        .responseBody(response.getEntity(String.class))
        .status(HttpStatus.valueOf(status))
        .statusCode(status)
        .statusMessage(response.getStatusInfo().getReasonPhrase())
        .build();
  }

  private com.sun.jersey.api.client.ClientResponse invoke(
      String url, String httpMethod, String body, String bearerToken) {
    WebResource webResource = restClient.resource(url);
    try {
      return webResource
          .type(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + bearerToken)
          .method(httpMethod, com.sun.jersey.api.client.ClientResponse.class, body);
    } catch (UniformInterfaceException | ClientHandlerException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Error calling airflow",
          String.format("Error calling airflow: %s", e.getMessage()),
          e);
    }
  }

  private static boolean isSuccess(int status) {
    return status >= HttpStatus.OK.value() && status < 300;
  }

  private boolean isUnauthorized(int status) {
    return status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value();
  }
}
