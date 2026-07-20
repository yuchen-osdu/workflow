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

package org.opengroup.osdu.workflow.provider.gcp.service;

import static java.lang.String.format;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Primary
public class GcpComposerAirflowApiClient implements IAirflowApiClient {

  private final AirflowConfig airflowConfig;
  private final ComposerClient iapClient;

  public GcpComposerAirflowApiClient(AirflowConfig airflowConfig, ComposerClient iapClient) {
    this.airflowConfig = airflowConfig;
    this.iapClient = iapClient;
    log.info(
        "Initialized Composer Airflow client and enabled {} authentication.",
        iapClient.getClass().getSimpleName());
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

    ClientResponse response = iapClient.sendAirflowRequest(httpMethod, url, body, rq);
    int status = response.getStatusCode();
    log.info("Received response status: {}.", status);
    if (status != 200) {
      throw new AppException(status, (String) response.getResponseBody(), errorMessage);
    }
    return response;
  }
}
