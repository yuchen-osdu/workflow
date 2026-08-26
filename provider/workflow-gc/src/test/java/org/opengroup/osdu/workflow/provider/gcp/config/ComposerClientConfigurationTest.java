/*
 *  Copyright 2020-2026 Google LLC
 *  Copyright 2020-2026 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.provider.gcp.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.provider.gcp.service.ComposerClient;
import org.opengroup.osdu.workflow.provider.gcp.service.ComposerIAMClient;
import org.opengroup.osdu.workflow.provider.gcp.service.ComposerIAPClient;

class ComposerClientConfigurationTest {

  private final AirflowConfig airflowConfig = mock(AirflowConfig.class);

  @Test
  void testCreatesIaapClientWhenConfigured() throws Exception {
    ComposerProperties properties = new ComposerProperties();
    properties.setClient("IAAP");
    ComposerClientConfiguration configuration = new ComposerClientConfiguration(properties);

    ComposerClient client = configuration.composerClient(airflowConfig);
    assertInstanceOf(ComposerIAPClient.class, client);
  }

  @Test
  void testCreatesIapClientWhenConfigured() throws Exception {
    ComposerProperties properties = new ComposerProperties();
    properties.setClient("IAP");
    ComposerClientConfiguration configuration = new ComposerClientConfiguration(properties);

    ComposerClient client = configuration.composerClient(airflowConfig);
    assertInstanceOf(ComposerIAPClient.class, client);
  }

  @Test
  void testCreatesIAMClientByDefault() {
    ComposerProperties properties = new ComposerProperties();
    ComposerClientConfiguration configuration = new ComposerClientConfiguration(properties);

    try {
      ComposerClient client = configuration.composerClient(airflowConfig);
      assertInstanceOf(ComposerIAMClient.class, client);
    } catch (Exception ignored) {
      // In env without GCP credentials GoogleCredentials.getApplicationDefault() will throw IOException
    }
  }
}
