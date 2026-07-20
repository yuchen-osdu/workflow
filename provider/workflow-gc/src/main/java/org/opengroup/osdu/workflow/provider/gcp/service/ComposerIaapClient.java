/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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
import static org.opengroup.osdu.workflow.provider.gcp.config.GcAirflowConfigConstants.IAAP;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = COMPOSER_CLIENT, havingValue = IAAP)
@RequiredArgsConstructor
public class ComposerIaapClient implements ComposerClient {

  private final GoogleIapHelper googleIapHelper;
  private final AirflowConfig airflowConfig;

  @Override
  public ClientResponse sendAirflowRequest(
      String httpMethod, String url, String stringData, WorkflowEngineRequest rq) {
    log.debug(
        "Calling airflow endpoint with Google API. Http method: {}, Endpoint: {}, request body: {}",
        httpMethod, url, getTruncatedData(stringData));
    String airflowUrl = this.airflowConfig.getUrl();
    String iapClientId = this.googleIapHelper.getIapClientId(airflowUrl);

    try {
      HttpRequest httpRequest =
          this.googleIapHelper.buildIapRequest(url, iapClientId, httpMethod, stringData);
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
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to send request.", errorMessage);
    }
  }

}
