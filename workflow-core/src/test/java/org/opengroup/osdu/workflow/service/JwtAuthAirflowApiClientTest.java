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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;

@ExtendWith(MockitoExtension.class)
class JwtAuthAirflowApiClientTest {

  private static final String BASE_URL = "https://af3";
  private static final String API_ENDPOINT = "api/v2/dags/x/dagRuns";
  private static final String API_URL = BASE_URL + "/" + API_ENDPOINT;
  private static final String TOKEN_URL = BASE_URL + "/auth/token";
  private static final String HTTP_METHOD = "POST";
  private static final String BODY = "{\"conf\": {}}";
  private static final String ERROR_MESSAGE = "trigger failed";
  private static final String TOKEN_RESPONSE = "{\"access_token\":\"header.payload.sig\"}";
  private static final String EXPECTED_BEARER = "Bearer header.payload.sig";
  private static final String OK_BODY = "{\"dag_run_id\": \"r1\"}";

  @Mock private Client restClient;
  @Mock private WorkflowEngineRequest rq;

  @Mock private WebResource tokenResource;
  @Mock private WebResource.Builder tokenBuilder;
  @Mock private com.sun.jersey.api.client.ClientResponse tokenResponse;

  @Mock private WebResource apiResource;
  @Mock private WebResource.Builder apiBuilder;

  private AirflowConfig airflowConfig;
  private JwtAuthAirflowApiClient client;

  @BeforeEach
  void setUp() {
    airflowConfig = new AirflowConfig();
    airflowConfig.setUrl(BASE_URL);
    airflowConfig.setUsername("admin");
    airflowConfig.setPassword("admin");
    client = new JwtAuthAirflowApiClient(restClient, airflowConfig);
  }

  private void stubTokenEndpoint(int status) {
    when(restClient.resource(eq(TOKEN_URL))).thenReturn(tokenResource);
    when(tokenResource.type(MediaType.APPLICATION_JSON)).thenReturn(tokenBuilder);
    when(tokenBuilder.post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString()))
        .thenReturn(tokenResponse);
    when(tokenResponse.getStatus()).thenReturn(status);
    when(tokenResponse.getEntity(String.class)).thenReturn(TOKEN_RESPONSE);
  }

  private void stubApiEndpoint() {
    when(restClient.resource(eq(API_URL))).thenReturn(apiResource);
    when(apiResource.type(MediaType.APPLICATION_JSON)).thenReturn(apiBuilder);
    when(apiBuilder.header(eq("Authorization"), anyString())).thenReturn(apiBuilder);
  }

  private com.sun.jersey.api.client.ClientResponse okResponse() {
    com.sun.jersey.api.client.ClientResponse resp =
        org.mockito.Mockito.mock(com.sun.jersey.api.client.ClientResponse.class);
    when(resp.getStatus()).thenReturn(200);
    when(resp.getEntity(String.class)).thenReturn(OK_BODY);
    when(resp.getType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
    when(resp.getStatusInfo())
        .thenReturn(
            new Response.StatusType() {
              @Override
              public int getStatusCode() {
                return 200;
              }

              @Override
              public Response.Status.Family getFamily() {
                return Response.Status.Family.SUCCESSFUL;
              }

              @Override
              public String getReasonPhrase() {
                return "OK";
              }
            });
    return resp;
  }

  private com.sun.jersey.api.client.ClientResponse statusResponse(int status) {
    com.sun.jersey.api.client.ClientResponse resp =
        org.mockito.Mockito.mock(com.sun.jersey.api.client.ClientResponse.class);
    when(resp.getStatus()).thenReturn(status);
    when(resp.getEntity(String.class)).thenReturn(OK_BODY);
    when(resp.getType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
    when(resp.getStatusInfo())
        .thenReturn(
            new Response.StatusType() {
              @Override
              public int getStatusCode() {
                return status;
              }

              @Override
              public Response.Status.Family getFamily() {
                return Response.Status.Family.SUCCESSFUL;
              }

              @Override
              public String getReasonPhrase() {
                return "Created";
              }
            });
    return resp;
  }

  @Test
  void should_treat201AsSuccess() {
    stubTokenEndpoint(201);
    stubApiEndpoint();
    com.sun.jersey.api.client.ClientResponse created = statusResponse(201);
    when(apiBuilder.method(
            eq(HTTP_METHOD), eq(com.sun.jersey.api.client.ClientResponse.class), eq(BODY)))
        .thenReturn(created);

    ClientResponse result = client.callAirflow(HTTP_METHOD, API_ENDPOINT, BODY, rq, ERROR_MESSAGE);

    assertEquals(201, result.getStatusCode());
    assertEquals(OK_BODY, result.getResponseBody());
  }

  @Test
  void should_acquireJwt_and_sendBearer_onCall() {
    stubTokenEndpoint(201);
    stubApiEndpoint();
    com.sun.jersey.api.client.ClientResponse ok = okResponse();
    when(apiBuilder.method(
            eq(HTTP_METHOD), eq(com.sun.jersey.api.client.ClientResponse.class), eq(BODY)))
        .thenReturn(ok);

    ClientResponse result = client.callAirflow(HTTP_METHOD, API_ENDPOINT, BODY, rq, ERROR_MESSAGE);

    assertEquals(OK_BODY, result.getResponseBody());
    assertEquals(200, result.getStatusCode());
    verify(apiBuilder).header("Authorization", EXPECTED_BEARER);
    verify(tokenBuilder, times(1))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_reuseCachedToken_acrossCalls() {
    stubTokenEndpoint(201);
    stubApiEndpoint();
    com.sun.jersey.api.client.ClientResponse ok1 = okResponse();
    com.sun.jersey.api.client.ClientResponse ok2 = okResponse();
    when(apiBuilder.method(
            eq(HTTP_METHOD), eq(com.sun.jersey.api.client.ClientResponse.class), eq(BODY)))
        .thenReturn(ok1, ok2);

    client.callAirflow(HTTP_METHOD, API_ENDPOINT, BODY, rq, ERROR_MESSAGE);
    client.callAirflow(HTTP_METHOD, API_ENDPOINT, BODY, rq, ERROR_MESSAGE);

    // Token fetched once and reused for the second call (default TTL keeps it valid).
    verify(tokenBuilder, times(1))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_refreshToken_andRetryOnce_when401() {
    stubTokenEndpoint(201);
    stubApiEndpoint();

    com.sun.jersey.api.client.ClientResponse unauthorized =
        org.mockito.Mockito.mock(com.sun.jersey.api.client.ClientResponse.class);
    when(unauthorized.getStatus()).thenReturn(401);
    com.sun.jersey.api.client.ClientResponse ok = okResponse();
    when(apiBuilder.method(
            eq(HTTP_METHOD), eq(com.sun.jersey.api.client.ClientResponse.class), eq(BODY)))
        .thenReturn(unauthorized, ok);

    ClientResponse result = client.callAirflow(HTTP_METHOD, API_ENDPOINT, BODY, rq, ERROR_MESSAGE);

    assertEquals(200, result.getStatusCode());
    // First fetch (cold cache) + forced refresh after 401 = 2 token requests.
    verify(tokenBuilder, times(2))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_throwAppException_whenTokenAcquisitionFails() {
    when(restClient.resource(eq(TOKEN_URL))).thenReturn(tokenResource);
    when(tokenResource.type(MediaType.APPLICATION_JSON)).thenReturn(tokenBuilder);
    when(tokenBuilder.post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString()))
        .thenReturn(tokenResponse);
    when(tokenResponse.getStatus()).thenReturn(401);
    when(tokenResponse.getEntity(String.class)).thenReturn("{\"detail\":\"bad creds\"}");

    AppException ex =
        assertThrows(
            AppException.class,
            () -> client.callAirflow(HTTP_METHOD, API_ENDPOINT, BODY, rq, ERROR_MESSAGE));

    assertEquals(401, ex.getError().getCode());
  }
}
