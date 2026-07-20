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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.PropertyException;

@Component
public class AwsServiceConfig {

  @Value("${aws.region}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String amazonRegion;

  @Value("${aws.airflow.api.mode}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String airflowApiMode;

  @Value("${aws.airflow.api.sqs.queue.url}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String workflowQueueUrl;

  @Value("${aws.airflow.api.http.baseUrl}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String airflowBaseUrl;


  // @Value("${aws.ssm}")
  // @Getter()
  // @Setter(AccessLevel.PROTECTED)
  // public Boolean ssmEnabled;

  /*@Inject
  protected JaxRsDpsLog logger;*/

  @PostConstruct
  public void init() throws PropertyException {
    // if (ssmEnabled) {
    //   SSMConfig ssmConfig = new SSMConfig();
    //   ParameterStorePropertySource ssm = ssmConfig.amazonSSM();

    //   try {
    //    //set properties from SSM here
    //   } catch (Exception e) {
    //     System.out.println(String.format("SSM property %s not found", parameter));
    //   }
    // }

    switch (airflowApiMode) {
      case AwsAirflowApiMode.HTTP:
        if (StringUtils.isBlank(airflowBaseUrl))
          throw new PropertyException("aws.airflow.api.http.baseUrl must be set when api mode is http");
        break;
      case AwsAirflowApiMode.SQS:
        if (StringUtils.isBlank(workflowQueueUrl))
          throw new PropertyException("aws.airflow.api.sqs.queue.url must be set when api mode is sqs");
        break;
      default:
          throw new PropertyException("Unsupported Airflow API mode set. cannot start. Must either be http or sqs");

    }
  }

}

