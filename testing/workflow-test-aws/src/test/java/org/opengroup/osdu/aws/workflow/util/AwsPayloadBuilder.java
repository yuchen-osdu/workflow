/**
 *  Copyright © 2026 Amazon Web Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */

package org.opengroup.osdu.aws.workflow.util;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;

/**
 * AWS-specific payload helpers for integration tests.
 *
 * <p>The shared {@link org.opengroup.osdu.workflow.util.PayloadBuilder} sends
 * the static {@code CREATE_WORKFLOW_WORKFLOW_NAME} in the create-workflow payload,
 * which matches a deployed Airflow DAG on Azure/CIMPL/GC/IBM environments.
 *
 * <p>On AWS, workflows are persisted in a DynamoDB table without a strict DAG-name
 * binding. Prior failed runs can leave stale rows in the table, causing subsequent
 * {@code POST /workflow} calls to return 409 Conflict. Appending a UUID per run
 * avoids these cross-run collisions while remaining compatible with the AWS
 * workflow service.
 *
 * <p>Scoping this to AWS only keeps Azure/CIMPL behavior unchanged, since those
 * environments require the workflow name to match an existing Airflow DAG.
 */
public final class AwsPayloadBuilder {

  private AwsPayloadBuilder() {}

  /**
   * Builds a valid create-workflow payload with a UUID-suffixed workflow name,
   * to avoid 409 Conflict across test runs against a shared DynamoDB table.
   */
  public static String buildCreateWorkflowValidPayloadWithUniqueName() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workflowName", uniqueWorkflowName());
    payload.put("registrationInstructions", new HashMap<String, String>());
    payload.put("description", "Test workflow record for integration tests.");
    return new Gson().toJson(payload);
  }

  /** Returns {@code <CREATE_WORKFLOW_WORKFLOW_NAME>-<UUID>}. */
  public static String uniqueWorkflowName() {
    return CREATE_WORKFLOW_WORKFLOW_NAME + "-" + UUID.randomUUID().toString();
  }
}
