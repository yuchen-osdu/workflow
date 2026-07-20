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

package org.opengroup.osdu.workflow.util;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.secret.SecretAPIConfig;
import org.opengroup.osdu.core.common.secret.SecretClientFactory;
import org.opengroup.osdu.core.common.secret.SecretClientFactoryImpl;
import org.opengroup.osdu.workflow.config.WorkflowConfigurationProperties;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecretFactory extends AbstractFactoryBean<SecretClientFactory> {

  private final WorkflowConfigurationProperties serviceConfig;

  @Override
  public Class<?> getObjectType() {
    return SecretClientFactory.class;
  }

  @Override
  protected SecretClientFactory createInstance() {
    SecretAPIConfig apiConfig =
        SecretAPIConfig.builder()
            .secretApi(serviceConfig.getSecretApi())
            .domain(serviceConfig.getDomain())
            .build();
    return new SecretClientFactoryImpl(apiConfig);
  }
}
