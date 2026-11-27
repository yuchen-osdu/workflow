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
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;

@ExtendWith(MockitoExtension.class)
class Airflow3TokenClientTest {

  private static final String BASE_URL = "https://af3";
  private static final String TOKEN_URL = BASE_URL + "/auth/token";

  @Mock private Client restClient;
  @Mock private WebResource tokenResource;
  @Mock private WebResource.Builder tokenBuilder;
  @Mock private com.sun.jersey.api.client.ClientResponse tokenResponse;

  private AirflowConfig airflowConfig;
  private Airflow3TokenClient tokenClient;

  @BeforeEach
  void setUp() {
    airflowConfig = new AirflowConfig();
    airflowConfig.setUrl(BASE_URL);
    airflowConfig.setUsername("admin");
    airflowConfig.setPassword("admin");
    tokenClient = new Airflow3TokenClient(restClient, airflowConfig);
  }

  private void stubTokenPost(int status, String responseBody) {
    when(restClient.resource(eq(TOKEN_URL))).thenReturn(tokenResource);
    when(tokenResource.type(MediaType.APPLICATION_JSON)).thenReturn(tokenBuilder);
    when(tokenBuilder.post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString()))
        .thenReturn(tokenResponse);
    when(tokenResponse.getStatus()).thenReturn(status);
    when(tokenResponse.getEntity(String.class)).thenReturn(responseBody);
  }

  @Test
  void should_returnToken_andCache_onSuccess() {
    stubTokenPost(200, "{\"access_token\":\"header.payload.sig\"}");

    assertEquals("header.payload.sig", tokenClient.token());
    // Second call reuses the cached token (still within the effective TTL).
    assertEquals("header.payload.sig", tokenClient.token());
    verify(tokenBuilder, times(1))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_throw_when_accessTokenMissing() {
    stubTokenPost(200, "{\"token_type\":\"bearer\"}");

    AppException ex = assertThrows(AppException.class, () -> tokenClient.token());
    assertEquals(500, ex.getError().getCode());
  }

  @Test
  void should_throw_when_accessTokenBlank() {
    stubTokenPost(200, "{\"access_token\":\"   \"}");

    AppException ex = assertThrows(AppException.class, () -> tokenClient.token());
    assertEquals(500, ex.getError().getCode());
  }

  @Test
  void should_throw_when_responseIsMalformedJson() {
    stubTokenPost(200, "not-json");

    AppException ex = assertThrows(AppException.class, () -> tokenClient.token());
    assertEquals(500, ex.getError().getCode());
  }

  @Test
  void should_propagateStatus_when_tokenEndpointReturns403() {
    stubTokenPost(403, "{\"detail\":\"forbidden\"}");

    AppException ex = assertThrows(AppException.class, () -> tokenClient.token());
    assertEquals(403, ex.getError().getCode());
  }

  @Test
  void should_wrapAsAppException_when_networkFails() {
    when(restClient.resource(eq(TOKEN_URL))).thenReturn(tokenResource);
    when(tokenResource.type(MediaType.APPLICATION_JSON)).thenReturn(tokenBuilder);
    when(tokenBuilder.post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString()))
        .thenThrow(new ClientHandlerException("connection refused"));

    AppException ex = assertThrows(AppException.class, () -> tokenClient.token());
    assertEquals(500, ex.getError().getCode());
  }

  @Test
  void should_refreshIfStale_onlyOnce_forConcurrentSameTokenCallers() {
    stubTokenPost(200, "{\"access_token\":\"t1\"}");
    String initial = tokenClient.token();
    assertEquals("t1", initial);

    // Next fetch returns a rotated token; two callers both saw the same stale token rejected.
    when(tokenResponse.getEntity(String.class)).thenReturn("{\"access_token\":\"t2\"}");
    String first = tokenClient.refreshIfStale(initial);
    String second = tokenClient.refreshIfStale(initial); // already refreshed -> no second fetch

    assertEquals("t2", first);
    assertEquals("t2", second);
    // 1 initial + 1 refresh = 2 token requests (not 3).
    verify(tokenBuilder, times(2))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_cacheUntilJwtExp_when_expClaimIsFarInFuture() {
    String jwt = jwtWithExp(Instant.now().plus(Duration.ofHours(10)));
    stubTokenPost(200, "{\"access_token\":\"" + jwt + "\"}");

    assertEquals(jwt, tokenClient.token());
    // exp is well beyond the refresh skew -> reused without a second /auth/token call.
    assertEquals(jwt, tokenClient.token());
    verify(tokenBuilder, times(1))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_refetch_when_jwtExpAlreadyPast() {
    String jwt = jwtWithExp(Instant.now().minus(Duration.ofMinutes(1)));
    stubTokenPost(200, "{\"access_token\":\"" + jwt + "\"}");

    assertEquals(jwt, tokenClient.token());
    // exp already elapsed -> token judged invalid -> a fresh /auth/token on the next call.
    tokenClient.token();
    verify(tokenBuilder, times(2))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  @Test
  void should_refetch_when_jwtExpWithinRefreshSkew() {
    // exp only 2 minutes out is inside the 5-minute refresh skew -> treated as already stale.
    String jwt = jwtWithExp(Instant.now().plus(Duration.ofMinutes(2)));
    stubTokenPost(200, "{\"access_token\":\"" + jwt + "\"}");

    assertEquals(jwt, tokenClient.token());
    tokenClient.token();
    verify(tokenBuilder, times(2))
        .post(eq(com.sun.jersey.api.client.ClientResponse.class), anyString());
  }

  private static String jwtWithExp(Instant exp) {
    String header = base64Url("{\"alg\":\"none\"}");
    String payload = base64Url("{\"exp\":" + exp.getEpochSecond() + "}");
    return header + "." + payload + ".";
  }

  private static String base64Url(String json) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }
}
