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

package org.opengroup.osdu.workflow.workflow.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.AIRFLOW3_EXPECTED_VERSION_PREFIX;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.client.HttpResponse;
import org.opengroup.osdu.core.test.service.ServiceType;
import org.opengroup.osdu.workflow.util.BaseWorkflowAcceptanceTest;
import org.opengroup.osdu.workflow.util.TestAirflow3;

/**
 * Airflow 3-specific acceptance tests. When the Workflow Service under test is configured with
 * Airflow 3 as its engine, the shared acceptance suite ({@code WorkflowRunV3IntegrationTests})
 * already exercises the trigger/status/run lifecycle against the AF3 {@code api/v2} endpoints, so
 * those behaviors are intentionally NOT duplicated here. This class holds only assertions that are
 * unique to Airflow 3 and would fail on an Airflow 2 backend.
 *
 * <p>Gated by {@code AIRFLOW3_TESTS_ENABLED=true} via {@link TestAirflow3}; skipped otherwise so the
 * shared suite stays green against Airflow 2-only deployments.
 */
public final class WorkflowRunAirflow3IntegrationTests extends BaseWorkflowAcceptanceTest {

  private static final String INFO_PATH = "info";
  private static final String CONNECTED_OUTER_SERVICES_FIELD = "connectedOuterServices";
  private static final String SERVICE_NAME_FIELD = "name";
  private static final String SERVICE_VERSION_FIELD = "version";
  // Matches AirflowResolverImpl.INTERNAL_AIRFLOW; the /info entry that carries the engine version
  // the Workflow Service is actually talking to.
  private static final String INTERNAL_AIRFLOW_SERVICE_NAME = "Internal Airflow";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @TestAirflow3
  void serviceInfo_onAirflow3_reportsAirflow3EngineVersion() throws Exception {
    HttpResponse<String> response =
        send(UserType.PRIVILEGED_USER, ServiceType.WORKFLOW_V1, INFO_PATH, Method.GET);
    assertEquals(HttpStatus.SC_OK, response.statusCode(), response.toString());

    JsonNode connectedOuterServices =
        OBJECT_MAPPER.readTree(response.body()).get(CONNECTED_OUTER_SERVICES_FIELD);
    assertNotNull(
        connectedOuterServices,
        "connectedOuterServices must be present in the /info response");

    String internalAirflowVersion = null;
    for (JsonNode service : connectedOuterServices) {
      if (INTERNAL_AIRFLOW_SERVICE_NAME.equals(service.path(SERVICE_NAME_FIELD).asText())) {
        internalAirflowVersion = service.path(SERVICE_VERSION_FIELD).asText();
        break;
      }
    }
    assertNotNull(
        internalAirflowVersion,
        "Expected a '"
            + INTERNAL_AIRFLOW_SERVICE_NAME
            + "' entry in connectedOuterServices but found: "
            + connectedOuterServices);

    // The reported engine version distinguishes Airflow 3 from Airflow 2 (e.g. "2.10.5+composer").
    // Assert on the major-version prefix (configurable via AIRFLOW3_EXPECTED_VERSION_PREFIX) rather
    // than an exact build so the test stays robust across Airflow 3 patch releases and environments.
    assertTrue(
        internalAirflowVersion.startsWith(AIRFLOW3_EXPECTED_VERSION_PREFIX),
        "Internal Airflow version should report Airflow "
            + AIRFLOW3_EXPECTED_VERSION_PREFIX
            + ".x but was: "
            + internalAirflowVersion);
  }
}
