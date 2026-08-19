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

package org.opengroup.osdu.workflow.aws.config;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.aws.service.mwaa.MwaaInvokeRestApiClient;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineService;
import org.opengroup.osdu.workflow.service.AirflowV3WorkflowEngineServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mwaa.MwaaClient;

@Configuration
public class AwsMwaaEngineConfig {

  @Bean
  public MwaaClient mwaaClient(@Value("${aws.region}") String region) {
    return MwaaClient.builder().region(Region.of(region)).build();
  }

  @Bean
  @Primary
  public MwaaInvokeRestApiClient mwaaInvokeRestApiClient(
      MwaaClient mwaaClient, @Value("${aws.airflow.mwaa.environmentName}") String environmentName) {
    if (environmentName == null || environmentName.isBlank()) {
      throw new IllegalStateException(
          "MWAA environment name is not configured. Set the 'aws.airflow.mwaa.environmentName' "
              + "property (env var MWAA_ENVIRONMENT_NAME) to the target MWAA environment name.");
    }
    return new MwaaInvokeRestApiClient(mwaaClient, environmentName);
  }

  @Bean
  @Primary
  public IWorkflowEngineService awsMwaaWorkflowEngineService(
      MwaaInvokeRestApiClient mwaaInvokeRestApiClient, DpsHeaders dpsHeaders) {
    return new AirflowV3WorkflowEngineServiceImpl(mwaaInvokeRestApiClient, dpsHeaders);
  }
}
