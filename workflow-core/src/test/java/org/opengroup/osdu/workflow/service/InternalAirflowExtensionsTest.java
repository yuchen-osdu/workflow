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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.provider.interfaces.IWorkflowEngineExtension;

class InternalAirflowExtensionsTest {

  private final IWorkflowEngineExtension af2 = mock(IWorkflowEngineExtension.class);
  private final IWorkflowEngineExtension af3 = mock(IWorkflowEngineExtension.class);

  private InternalAirflowExtensions af3Enabled() {
    Map<String, IWorkflowEngineExtension> byVersion = new LinkedHashMap<>();
    byVersion.put(AirflowEngineVersions.V2, af2);
    byVersion.put(AirflowEngineVersions.V2_LEGACY_ALIAS, af2);
    byVersion.put(AirflowEngineVersions.V3, af3);
    return new InternalAirflowExtensions(byVersion, af2);
  }

  private InternalAirflowExtensions af2Only() {
    Map<String, IWorkflowEngineExtension> byVersion = new LinkedHashMap<>();
    byVersion.put(AirflowEngineVersions.V2, af2);
    byVersion.put(AirflowEngineVersions.V2_LEGACY_ALIAS, af2);
    return new InternalAirflowExtensions(byVersion, af2);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "airflow3, af3",
        "v3, af3",
        "' AIRFLOW3 ', af3",
        "airflow2, af2",
        "v2, af2",
        "v1, af2",
        "foo, af2",
        "'', af2",
        "NULL, af2"
      },
      nullValues = "NULL")
  void forEngineVersion_locksRoutingContract(String engineVersion, String expected) {
    IWorkflowEngineExtension expectedExt = "af3".equals(expected) ? af3 : af2;
    assertSame(expectedExt, af3Enabled().forEngineVersion(engineVersion));
  }

  @Test
  void hasVersionedRouting_trueWhenAf3Registered_falseForAf2Only() {
    assertTrue(af3Enabled().hasVersionedRouting());
    assertFalse(af2Only().hasVersionedRouting());
  }

  @Test
  void af2Only_alwaysResolvesToDefault_evenForAirflow3Key() {
    // With no AF3 extension registered, even an airflow3 key degrades to the default (AF2) extension.
    assertSame(af2, af2Only().forEngineVersion("airflow3"));
  }
}
