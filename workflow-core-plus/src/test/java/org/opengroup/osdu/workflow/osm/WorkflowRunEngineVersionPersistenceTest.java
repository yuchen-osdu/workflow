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

package org.opengroup.osdu.workflow.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.model.AirflowEngineVersions;
import org.opengroup.osdu.workflow.model.WorkflowRun;
import org.opengroup.osdu.workflow.osm.config.TypeMapperImpl;

/**
 * Locks the contract that core-plus persists {@link WorkflowRun#getEngineVersion()} so per-run
 * status/log/delete route to the engine that owns the run.
 *
 * <p>Core-plus persists {@code WorkflowRun} through OSM, whose {@code TypeMapper} serializes
 * entities with <b>Gson</b>. Gson serializes all non-transient fields via reflection and — unlike
 * Jackson — does <b>not</b> honor {@code @JsonIgnore}. The {@code @JsonIgnore} on
 * {@code engineVersion} therefore hides it from REST responses without preventing OSM persistence.
 * This test uses the <b>real</b> {@link TypeMapperImpl} Gson ({@code byClass(WorkflowRun.class)}) so
 * it exercises the actual OSM serialization config, guarding against a regression that would
 * silently stop persisting {@code engineVersion} and resurrect the orphaned-DAG failure mode.
 */
class WorkflowRunEngineVersionPersistenceTest {

  private final Gson gson = new TypeMapperImpl().byClass(WorkflowRun.class);

  @Test
  void engineVersion_roundTripsThroughOsmGson_despiteJsonIgnore() {
    WorkflowRun run =
        WorkflowRun.builder()
            .workflowId("wf")
            .runId("run-1")
            .engineVersion(AirflowEngineVersions.V3)
            .build();

    String serialized = gson.toJson(run);
    assertTrue(
        serialized.contains("engineVersion"),
        "OSM (Gson) must serialize engineVersion; got: " + serialized);

    WorkflowRun restored = gson.fromJson(serialized, WorkflowRun.class);
    assertEquals(AirflowEngineVersions.V3, restored.getEngineVersion());
  }

  @Test
  void engineVersion_persistsCanonicalAirflow2Value() {
    // The stamped value is already normalized by AirflowEngineVersionProvider (v2 -> airflow2), so
    // persistence stores the canonical identifier.
    WorkflowRun run =
        WorkflowRun.builder().runId("run-2").engineVersion(AirflowEngineVersions.V2).build();

    WorkflowRun restored = gson.fromJson(gson.toJson(run), WorkflowRun.class);
    assertEquals("airflow2", restored.getEngineVersion());
  }
}
