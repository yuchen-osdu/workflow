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

package org.opengroup.osdu.workflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AirflowEngineVersionsTest {

  @Test
  void normalize_mapsAliasesToCanonical() {
    assertEquals(AirflowEngineVersions.V2, AirflowEngineVersions.normalize("v2"));
    assertEquals(AirflowEngineVersions.V2, AirflowEngineVersions.normalize("airflow2"));
    assertEquals(AirflowEngineVersions.V3, AirflowEngineVersions.normalize("v3"));
    assertEquals(AirflowEngineVersions.V3, AirflowEngineVersions.normalize("airflow3"));
    assertEquals(AirflowEngineVersions.V3, AirflowEngineVersions.normalize("  AIRFLOW3 "));
  }

  @Test
  void normalize_blankOrNull_defaultsToAirflow2() {
    assertEquals(AirflowEngineVersions.V2, AirflowEngineVersions.normalize(null));
    assertEquals(AirflowEngineVersions.V2, AirflowEngineVersions.normalize(""));
    assertEquals(AirflowEngineVersions.V2, AirflowEngineVersions.normalize("   "));
  }

  @Test
  void isAirflow3_trueForCanonicalAndShortAlias() {
    assertTrue(AirflowEngineVersions.isAirflow3("airflow3"));
    assertTrue(AirflowEngineVersions.isAirflow3("v3"));
    assertTrue(AirflowEngineVersions.isAirflow3(" AirFlow3 "));
  }

  @Test
  void isAirflow3_falseForAirflow2AndNull() {
    assertFalse(AirflowEngineVersions.isAirflow3("airflow2"));
    assertFalse(AirflowEngineVersions.isAirflow3("v2"));
    assertFalse(AirflowEngineVersions.isAirflow3(null));
    assertFalse(AirflowEngineVersions.isAirflow3(""));
  }

  @Test
  void isStableApi_trueForAirflow2And3Aliases_trimmed() {
    assertTrue(AirflowEngineVersions.isStableApi("airflow2"));
    assertTrue(AirflowEngineVersions.isStableApi("v2"));
    assertTrue(AirflowEngineVersions.isStableApi("airflow3"));
    assertTrue(AirflowEngineVersions.isStableApi("v3"));
    assertTrue(AirflowEngineVersions.isStableApi(" airflow3 "));
    assertTrue(AirflowEngineVersions.isStableApi(" V2 "));
  }

  @Test
  void isStableApi_falseForV1BlankAndNull() {
    // Blank/unset must be false so an unset osdu.airflow.version (Airflow 1 default) does not enable
    // the Airflow 2/3-only /latestInfo endpoint.
    assertFalse(AirflowEngineVersions.isStableApi("v1"));
    assertFalse(AirflowEngineVersions.isStableApi(null));
    assertFalse(AirflowEngineVersions.isStableApi(""));
    assertFalse(AirflowEngineVersions.isStableApi("   "));
  }

  @Test
  void resolveConfiguredVersion_respectsPrecedence() {
    assertEquals(AirflowEngineVersions.V3,
        AirflowEngineVersions.resolveConfiguredVersion("airflow3", null));
    // Explicit version wins over the legacy flag, even when contradictory.
    assertEquals(AirflowEngineVersions.V1,
        AirflowEngineVersions.resolveConfiguredVersion("v1", Boolean.TRUE));
    assertEquals(AirflowEngineVersions.V2,
        AirflowEngineVersions.resolveConfiguredVersion(null, Boolean.TRUE));
    assertEquals(AirflowEngineVersions.V1,
        AirflowEngineVersions.resolveConfiguredVersion(null, null));
    assertEquals(AirflowEngineVersions.V1,
        AirflowEngineVersions.resolveConfiguredVersion("  ", Boolean.FALSE));
  }

  @Test
  void resolvesToStableApi_followsResolvedEngineNotRawOr() {
    assertTrue(AirflowEngineVersions.resolvesToStableApi("airflow2", null));
    assertTrue(AirflowEngineVersions.resolvesToStableApi("airflow3", Boolean.FALSE));
    assertTrue(AirflowEngineVersions.resolvesToStableApi(null, Boolean.TRUE));
    // New-3: an explicit v1 override disables the AF2/3-only gate even if legacy version2=true.
    assertFalse(AirflowEngineVersions.resolvesToStableApi("v1", Boolean.TRUE));
    assertFalse(AirflowEngineVersions.resolvesToStableApi(null, null));
    assertFalse(AirflowEngineVersions.resolvesToStableApi(null, Boolean.FALSE));
  }
}
