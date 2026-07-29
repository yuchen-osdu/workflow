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

package org.opengroup.osdu.workflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;

class AirflowEngineVersionProviderTest {

  @Test
  void should_UseExplicitVersion_when_Set() {
    assertThat(new AirflowEngineVersionProvider("airflow3", null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V3);
    assertThat(new AirflowEngineVersionProvider("airflow2", null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V2);
    assertThat(new AirflowEngineVersionProvider("v1", null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V1);
  }

  @Test
  void should_NormalizeLegacyV2Alias_toCanonicalAirflow2() {
    assertThat(new AirflowEngineVersionProvider("v2", null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V2);
    assertThat(new AirflowEngineVersionProvider("  AIRFLOW3 ", null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V3);
  }

  @Test
  void should_HonourLegacyBoolean_when_VersionUnset() {
    assertThat(new AirflowEngineVersionProvider(null, Boolean.TRUE).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V2);
    assertThat(new AirflowEngineVersionProvider(null, Boolean.FALSE).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V1);
  }

  @Test
  void should_DefaultToAirflow1_when_NothingSet() {
    // Backward compatibility: neither property set has always meant the Airflow 1 experimental API.
    assertThat(new AirflowEngineVersionProvider(null, null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V1);
    assertThat(new AirflowEngineVersionProvider("  ", null).getConfiguredVersion())
        .isEqualTo(AirflowEngineVersions.V1);
  }
}
