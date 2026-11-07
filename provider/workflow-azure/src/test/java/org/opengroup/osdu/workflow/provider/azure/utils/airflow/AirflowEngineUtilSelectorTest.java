// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.workflow.provider.azure.utils.airflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

public class AirflowEngineUtilSelectorTest {

  @SuppressWarnings("unchecked")
  private ObjectProvider<IAirflowWorkflowEngineUtil> provider(IAirflowWorkflowEngineUtil v3) {
    ObjectProvider<IAirflowWorkflowEngineUtil> p = mock(ObjectProvider.class);
    when(p.getIfAvailable()).thenReturn(v3);
    return p;
  }

  @Test
  void should_RouteAirflow3ToV3_when_V3Available() {
    IAirflowWorkflowEngineUtil base = mock(IAirflowWorkflowEngineUtil.class);
    IAirflowWorkflowEngineUtil v3 = mock(IAirflowWorkflowEngineUtil.class);

    AirflowEngineUtilSelector selector =
        new AirflowEngineUtilSelector(List.of(base, v3), provider(v3));

    assertTrue(selector.isV3Available());
    assertEquals(v3, selector.utilForVersion("airflow3"));
    assertEquals(v3, selector.utilForVersion("AIRFLOW3"));
    assertEquals(base, selector.utilForVersion("airflow2"));
    assertEquals(base, selector.utilForVersion("v2"));
    assertEquals(base, selector.utilForVersion(null));
    assertEquals(v3, selector.getDefaultUtil());
  }

  @Test
  void should_RouteUnknownAndLegacyVersionsToBase_when_V3Available() {
    IAirflowWorkflowEngineUtil base = mock(IAirflowWorkflowEngineUtil.class);
    IAirflowWorkflowEngineUtil v3 = mock(IAirflowWorkflowEngineUtil.class);

    AirflowEngineUtilSelector selector =
        new AirflowEngineUtilSelector(List.of(base, v3), provider(v3));

    // Consistent with the core PerRunAirflowWorkflowEngineService and InternalAirflowExtensions
    // routers: any non-airflow3 version — legacy v1, an unknown/typo'd value, or blank — resolves to
    // the base (Airflow 2) util, never the Airflow 3 util.
    assertEquals(base, selector.utilForVersion("v1"));
    assertEquals(base, selector.utilForVersion("foo"));
    assertEquals(base, selector.utilForVersion(""));
    assertEquals(base, selector.utilForVersion("   "));
  }

  @Test
  void should_FallBackToBase_when_V3NotAvailable() {
    IAirflowWorkflowEngineUtil base = mock(IAirflowWorkflowEngineUtil.class);

    AirflowEngineUtilSelector selector =
        new AirflowEngineUtilSelector(List.of(base), provider(null));

    assertFalse(selector.isV3Available());
    assertEquals(base, selector.utilForVersion(null));
    assertEquals(base, selector.utilForVersion("airflow2"));
  }

  @Test
  void should_FallBackToBase_when_Airflow3RequestedButV3Unavailable() {
    IAirflowWorkflowEngineUtil base = mock(IAirflowWorkflowEngineUtil.class);

    AirflowEngineUtilSelector selector =
        new AirflowEngineUtilSelector(List.of(base), provider(null));

    // A run stamped airflow3 after AF3 was disabled/rolled back must degrade to the base util
    // (Airflow answers 404) rather than hard-failing every status/delete call with a 500.
    assertEquals(base, selector.utilForVersion("airflow3"));
    assertEquals(base, selector.getDefaultUtil());
  }

  @Test
  void should_RouteV3Alias_toV3_when_V3Available() {
    IAirflowWorkflowEngineUtil base = mock(IAirflowWorkflowEngineUtil.class);
    IAirflowWorkflowEngineUtil v3 = mock(IAirflowWorkflowEngineUtil.class);

    AirflowEngineUtilSelector selector =
        new AirflowEngineUtilSelector(List.of(base, v3), provider(v3));

    // The short "v3" alias normalizes to airflow3 and must route to the AF3 util, matching the
    // config-level osdu.airflow.version=v3 alias.
    assertEquals(v3, selector.utilForVersion("v3"));
    assertEquals(v3, selector.utilForVersion(" V3 "));
  }

  @Test
  void should_DispatchByRequestEngineVersion() {
    IAirflowWorkflowEngineUtil base = mock(IAirflowWorkflowEngineUtil.class);
    IAirflowWorkflowEngineUtil v3 = mock(IAirflowWorkflowEngineUtil.class);

    AirflowEngineUtilSelector selector =
        new AirflowEngineUtilSelector(List.of(base, v3), provider(v3));

    assertEquals(
        v3,
        selector.utilFor(
            org.opengroup.osdu.workflow.model.WorkflowEngineRequest.builder()
                .engineVersion("airflow3")
                .build()));
    assertEquals(
        base,
        selector.utilFor(
            org.opengroup.osdu.workflow.model.WorkflowEngineRequest.builder()
                .engineVersion("airflow2")
                .build()));
    // A legacy run with no engine version and a null request both fall back to the base util.
    assertEquals(
        base,
        selector.utilFor(
            org.opengroup.osdu.workflow.model.WorkflowEngineRequest.builder().build()));
    assertEquals(base, selector.utilFor(null));
  }

  @Test
  void should_Throw_when_NoBaseUtilRegistered() {
    assertThrows(
        IllegalStateException.class,
        () -> new AirflowEngineUtilSelector(Collections.emptyList(), provider(null)));
  }
}
