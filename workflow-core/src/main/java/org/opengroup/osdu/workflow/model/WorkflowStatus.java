/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStatus {

  @JsonProperty(Fields.WORKFLOW_ID)
  @Schema(description = "Workflow id for the workflow", type = "string")
  String workflowId;

  @JsonProperty(Fields.AIRFLOW_RUN_ID)
  @Schema(description = "Run id for the workflow", type = "string")
  String airflowRunId;

  @JsonProperty(Fields.WORKFLOW_STATUS_TYPE)
  @Schema(description = "Task execution status", type = "string", allowableValues = {"INPROGRESS", "PARTIAL_SUCCESS", "SUCCESS", "FAILED", "SUBMITTED"})
  WorkflowStatusType workflowStatusType;

  @JsonProperty(Fields.SUBMITTED_AT)
  @Schema(description = "Start timestamp of the workflow run.Epoch time stamp")
  Date submittedAt;

  @JsonProperty(Fields.SUBMITTED_BY)
  @Schema(description = "System captured user details which triggered the  run.", type = "string")
  String submittedBy;

  public static final class Fields {

    public static final String WORKFLOW_ID = "WorkflowID";
    public static final String AIRFLOW_RUN_ID = "AirflowRunID";
    public static final String WORKFLOW_STATUS_TYPE = "Status";
    public static final String SUBMITTED_AT = "SubmittedAt";
    public static final String SUBMITTED_BY = "SubmittedBy";

    private Fields() {
    }
  }

}
