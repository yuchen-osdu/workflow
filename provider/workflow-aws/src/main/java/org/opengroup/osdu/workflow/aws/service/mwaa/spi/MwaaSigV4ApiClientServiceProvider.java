/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.workflow.aws.service.mwaa.spi;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.workflow.aws.service.mwaa.MwaaInvokeRestApiClient;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.opengroup.osdu.workflow.service.spi.IAirflowApiClientServiceProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.mwaa.MwaaClient;

/**
 * SPI registration of the MWAA SigV4 client, mirroring {@code
 * GcpComposerAirflowApiClientServiceProvider}. This is not the wiring used for the default/
 * internal MWAA environment — {@code AwsMwaaEngineConfig} binds that one directly as a {@code
 * @Primary} bean instead of going through the SPI factory. This provider exists so {@code
 * AirflowApiClientFactory} can build additional MWAA-backed clients for externally-registered
 * ({@code externalAirflowSecret}) workflows, and to give any future configurable Airflow-client-
 * type work a ready-made non-JWT provider to reference.
 */
@Component
@RequiredArgsConstructor
public class MwaaSigV4ApiClientServiceProvider implements IAirflowApiClientServiceProvider {

  private final MwaaClient mwaaClient;

  @Override
  public boolean supports(String airflowApiClientType) {
    return MwaaSigV4Constants.MWAA_SIG_V4.equalsIgnoreCase(airflowApiClientType);
  }

  @Override
  public IAirflowApiClient create(AirflowConfig airflowConfig) {
    return new MwaaInvokeRestApiClient(mwaaClient, airflowConfig.getUrl());
  }
}
