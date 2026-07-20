/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.provider.gcp.service;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opengroup.osdu.workflow.logging.LoggerUtils.getTruncatedData;
import static org.opengroup.osdu.workflow.provider.gcp.config.GcAirflowConfigConstants.COMPOSER_CLIENT;
import static org.opengroup.osdu.workflow.provider.gcp.config.GcAirflowConfigConstants.V2;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = COMPOSER_CLIENT, havingValue = V2)
public class ComposerV2Client implements ComposerClient {

  public static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
  private final HttpTransport httpTransport = new NetHttpTransport();

  private final GoogleCredentials credentials;

  public ComposerV2Client() throws IOException {
    try {
      credentials = GoogleCredentials.getApplicationDefault()
          .createScoped(ImmutableSet.of(CLOUD_PLATFORM_SCOPE));
    } catch (IOException e) {
      log.error("Unable to get default application credentials!", e);
      throw e;
    }
  }

  @Override
  public ClientResponse sendAirflowRequest(String httpMethod, String url, String stringData,
      WorkflowEngineRequest rq) {
    log.debug(
        "Calling airflow endpoint with Google API. Http method: {}, Endpoint: {}, request body: {}",
        httpMethod, url, getTruncatedData(stringData));

    InputStreamContent inputStreamContent = null;

    if (Objects.nonNull(stringData)) {
      inputStreamContent =
          new InputStreamContent(MediaType.APPLICATION_JSON_VALUE,
              new ByteArrayInputStream(stringData.getBytes()));
    }

    try {
      HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(credentials);

      HttpRequest httpRequest = httpTransport
          .createRequestFactory(httpRequestInitializer)
          .buildRequest(httpMethod, new GenericUrl(url), inputStreamContent);

      HttpResponse response = httpRequest.execute();
      String content = IOUtils.toString(response.getContent(), UTF_8);

      return ClientResponse.builder()
          .contentEncoding(response.getContentEncoding())
          .contentType(response.getContentType())
          .responseBody(content)
          .status(HttpStatus.OK)
          .statusCode(response.getStatusCode())
          .statusMessage(response.getStatusMessage())
          .build();

    } catch (HttpResponseException e) {
      String errorMessage = format("Unable to send request to Airflow. %s", e.getMessage());
      log.error(errorMessage, e);
      throw new AppException(e.getStatusCode(), "Failed to send request.", errorMessage);
    } catch (IOException e) {
      String errorMessage = format("Unable to send request to Airflow. %s", e.getMessage());
      log.error(errorMessage, e);
      throw new AppException(500, "Failed to send request.", errorMessage);
    }
  }
}
