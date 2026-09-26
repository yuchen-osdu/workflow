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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.springframework.http.HttpStatus;

/**
 * Acquires and caches a JWT bearer token for Airflow 3's native {@code api/v2} auth manager
 * (SimpleAuthManager / FabAuthManager) by exchanging username/password at {@code POST /auth/token}.
 *
 * <p>Shared by the Airflow 3 clients across providers (core {@code JwtAuthAirflowApiClient} and the
 * Azure engine) so the token lifecycle is implemented once:
 *
 * <ul>
 *   <li>token validity is driven by the JWT's own {@code exp} claim (minus {@link #REFRESH_SKEW});
 *       when {@code exp} cannot be parsed (opaque/non-JWT token) the client falls back to a fixed
 *       {@value #TOKEN_TTL_HOURS}h TTL from acquisition time;
 *   <li>{@link #refreshIfStale(String)} is herd-safe — when many callers see a 401 for the same
 *       token, only the first triggers a new {@code /auth/token} request; the rest reuse it. The
 *       401 retry remains a safety net, not the primary expiry mechanism.
 * </ul>
 */
@Slf4j
public class Airflow3TokenClient {

  private static final String TOKEN_ENDPOINT = "auth/token";
  private static final String USERNAME_FIELD = "username";
  private static final String PASSWORD_FIELD = "password";
  private static final String ACCESS_TOKEN_FIELD = "access_token";
  private static final String EXP_CLAIM = "exp";

  /** Fallback lifetime used only when the token carries no parseable {@code exp} claim. */
  private static final long TOKEN_TTL_HOURS = 1L;
  private static final Duration TOKEN_TTL = Duration.ofHours(TOKEN_TTL_HOURS);
  /**
   * Refresh this much earlier than the token's expiry so a token judged "valid" locally cannot
   * expire in transit / on the Airflow side. Absorbs small clock skew and request latency.
   */
  private static final Duration REFRESH_SKEW = Duration.ofMinutes(5L);

  private final Client restClient;
  private final AirflowConfig airflowConfig;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private volatile String cachedToken;
  private volatile Instant tokenExpiresAt = Instant.EPOCH;

  public Airflow3TokenClient(Client restClient, AirflowConfig airflowConfig) {
    this.restClient = restClient;
    this.airflowConfig = airflowConfig;
  }

  /** Returns a valid bearer token, acquiring a new one when the cache is empty or expired. */
  public String token() {
    if (isTokenValid()) {
      return cachedToken;
    }
    synchronized (this) {
      if (!isTokenValid()) {
        fetchToken();
      }
      return cachedToken;
    }
  }

  /**
   * Forces a refresh only if the cached token is still the {@code staleToken} that a caller just
   * saw rejected. If another thread already refreshed, the new token is returned without a second
   * {@code /auth/token} request (avoids a thundering herd on concurrent 401s).
   */
  public String refreshIfStale(String staleToken) {
    synchronized (this) {
      if (staleToken == null || staleToken.equals(cachedToken) || cachedToken == null) {
        fetchToken();
      }
      return cachedToken;
    }
  }

  private boolean isTokenValid() {
    return cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minus(REFRESH_SKEW));
  }

  /**
   * Derives the token's expiry from its JWT {@code exp} claim, falling back to a fixed TTL from now
   * when the token is opaque or the claim cannot be parsed.
   */
  private Instant computeExpiry(String accessToken) {
    return parseJwtExpiry(accessToken).orElseGet(() -> Instant.now().plus(TOKEN_TTL));
  }

  private Optional<Instant> parseJwtExpiry(String jwt) {
    try {
      String[] parts = jwt.split("\\.");
      if (parts.length < 2) {
        return Optional.empty();
      }
      String segment = parts[1];
      int padding = (4 - segment.length() % 4) % 4;
      byte[] payload = Base64.getUrlDecoder().decode(segment + "=".repeat(padding));
      JsonNode claims = objectMapper.readTree(payload);
      if (claims.hasNonNull(EXP_CLAIM) && claims.get(EXP_CLAIM).canConvertToLong()) {
        return Optional.of(Instant.ofEpochSecond(claims.get(EXP_CLAIM).asLong()));
      }
    } catch (Exception e) {
      log.debug(
          "Could not parse 'exp' from Airflow JWT; falling back to fixed {}h TTL. Reason: {}",
          TOKEN_TTL_HOURS,
          e.getMessage());
    }
    return Optional.empty();
  }

  private void fetchToken() {
    String tokenUrl = format("%s/%s", airflowConfig.getUrl(), TOKEN_ENDPOINT);
    log.info("Acquiring Airflow 3 JWT from {}", tokenUrl);

    JSONObject requestBody = new JSONObject();
    requestBody.put(USERNAME_FIELD, airflowConfig.getUsername());
    requestBody.put(PASSWORD_FIELD, airflowConfig.getPassword());

    ClientResponse response;
    try {
      response =
          restClient
              .resource(tokenUrl)
              .type(MediaType.APPLICATION_JSON)
              .post(ClientResponse.class, requestBody.toString());
    } catch (UniformInterfaceException | ClientHandlerException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Error acquiring Airflow token",
          String.format("Error acquiring Airflow token: %s", e.getMessage()),
          e);
    }

    int status = response.getStatus();
    if (!isSuccess(status)) {
      String responseBody = response.getEntity(String.class);
      throw new AppException(
          status, responseBody, String.format("Failed to acquire Airflow token (status %d)", status));
    }

    try {
      JsonNode json = objectMapper.readValue(response.getEntity(String.class), JsonNode.class);
      if (!json.hasNonNull(ACCESS_TOKEN_FIELD)) {
        throw new AppException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Malformed Airflow token response",
            String.format("Airflow token response did not contain '%s'", ACCESS_TOKEN_FIELD));
      }
      String accessToken = json.get(ACCESS_TOKEN_FIELD).asText();
      if (accessToken == null || accessToken.isBlank()) {
        throw new AppException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Malformed Airflow token response",
            String.format("Airflow token response contained a blank '%s'", ACCESS_TOKEN_FIELD));
      }
      this.cachedToken = accessToken;
      this.tokenExpiresAt = computeExpiry(accessToken);
      log.info(
          "Acquired Airflow 3 JWT; valid until {} (refreshing {}m early)",
          tokenExpiresAt,
          REFRESH_SKEW.toMinutes());
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Error parsing Airflow token response",
          String.format("Error parsing Airflow token response: %s", e.getMessage()),
          e);
    }
  }

  private static boolean isSuccess(int status) {
    return status >= HttpStatus.OK.value() && status < 300;
  }
}
