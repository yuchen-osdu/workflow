/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package org.opengroup.osdu.workflow.workflow.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.util.HTTPClient;
import org.opengroup.osdu.workflow.util.v3.TestBase;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.CREATE_WORKFLOW_WORKFLOW_NAME;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_ALL_WORKFLOW_PREFIX;
import static org.opengroup.osdu.workflow.consts.TestConstants.GET_WORKFLOW_URL;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_CORRELATION_ID;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectDag;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithIncorrectWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowPayloadWithNoWorkflowName;
import static org.opengroup.osdu.workflow.util.PayloadBuilder.buildCreateWorkflowValidPayload;

public abstract class WorkflowV3IntegrationTests extends TestBase {

  public static final String WORKFLOW_NOT_FOUND_MESSAGE = "Workflow: %s doesn't exist";
  private static String CORRELATION_ID = "test-correlation-id";

  @Test
  public void shouldReturnSuccessWhenGivenValidRequestWorkflowCreate() throws Exception {
    String responseBody = createWorkflow();
    createdWorkflows.add(getWorkflowInfoFromCreateWorkflowResponseBody(responseBody));
  }

  @Test
  public void shouldReturnBadRequestWhenInvalidDagNameWorkflowCreate() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowPayloadWithIncorrectDag(),
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void shouldReturnBadRequestWhenIncorrectWorkflowNameWorkflowCreate()
      throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowPayloadWithIncorrectWorkflowName(),
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void shouldContainCorrelationIdInResponseHeaders_whenGetListWorkflowForTenant_givenNoCorrelationIdInHeaders() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL,
        null,
        headers,
        client.getAccessToken()
    );
    assertTrue(response.getHeaders().containsKey(HEADER_CORRELATION_ID));
    assertTrue(StringUtils.isNotBlank(response.getHeaders().get(HEADER_CORRELATION_ID).get(0)));
  }

  @Test
  public void shouldContainCorrelationIdInResponseHeaders_whenGetListWorkflowForTenant_givenCorrelationIdInHeaders() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    Map<String, String> headersWithCorrelationId = new HashMap<>(headers);
    headersWithCorrelationId.put(HEADER_CORRELATION_ID, CORRELATION_ID);
    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL,
        null,
        headersWithCorrelationId,
        client.getAccessToken()
    );
    assertEquals(CORRELATION_ID, response.getHeaders().get(HEADER_CORRELATION_ID).get(0));
  }

  /**
   * GET ALL WORKFLOWS FOR TENANT INTEGRATION TESTS
   **/

  @Test
  public void getAllWorkflows_should_return200_when_getAllWorkflowsForTenant() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo =
        getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL + "?prefix=",
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    responseBody = response.getEntity(String.class);
    List<Object> list =
        new ObjectMapper().readValue(responseBody, ArrayList.class);
    assertTrue(!list.isEmpty());
  }

  @Test
  public void getAllWorkflows_should_return200_when_getAllWorkflowsForTenantWithEmptyPrefix() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo =
        getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL + GET_ALL_WORKFLOW_PREFIX,
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());

    responseBody = response.getEntity(String.class);
    List<Object> list =
        new ObjectMapper().readValue(responseBody, ArrayList.class);
    assertTrue(!list.isEmpty());
  }

  @Test
  public void getAllWorkflows_should_returnUnauthorized_when_notGivenAccessToken() {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL,
        null,
        headers,
        null
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void getAllWorkflows_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL,
        null,
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void getAllWorkflows_should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);
    ClientResponse response = client.send(
        HttpMethod.GET,
        CREATE_WORKFLOW_URL,
        null,
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void shouldReturnBadRequestWhenGetCompleteDetailsForWorkflow() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    String url = CREATE_WORKFLOW_URL + "/_" + getLastCreatedWorkflowName();
    ClientResponse response = client.send(
        HttpMethod.GET,
        url,
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  /**
   * GET WORKFLOW BY ID INTEGRATION TESTS
   **/

  @Test
  public void getWorkflowById_should_return200_when_givenValidWorkflowId() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    String actualWorkflowName = workflowInfo.get(WORKFLOW_NAME_FIELD);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_URL, actualWorkflowName),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    responseBody = response.getEntity(String.class);
    Map<String, Object> result = new ObjectMapper().readValue(responseBody, HashMap.class);
    assertTrue(!result.isEmpty());
  }

  @Test
  public void getWorkflowById_should_returnNotFound_when_givenInvalidWorkflowName() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_URL, INVALID_WORKFLOW_NAME),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void getWorkflowById_should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_URL, getLastCreatedWorkflowName()),
        null,
        headers,
        null
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void getWorkflowById_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_URL, getLastCreatedWorkflowName()),
        null,
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void getWorkflowById_should_returnForbidden_when_givenInvalidPartition() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(GET_WORKFLOW_URL, getLastCreatedWorkflowName()),
        null,
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  /** POST CREATE WORKFLOW INTEGRATION TESTS **/

  @Test
  public void createWorkflow_should_returnWorkflowExists_when_givenDuplicateCreateWorkflowRequest() throws Exception{
    String payload = buildCreateWorkflowValidPayload();

    ClientResponse firstResponse = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        payload,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_OK, firstResponse.getStatus(), firstResponse.toString());
    String responseBody = firstResponse.getEntity(String.class);

    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    ClientResponse duplicateResponse = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        payload,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_CONFLICT, duplicateResponse.getStatus());
  }

  @Test
  public void createWorkflow_should_returnBadRequest_when_givenInvalidRequestWithNoWorkflowName() throws Exception{
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowPayloadWithNoWorkflowName(),
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void createWorkflow_should_returnForbidden_when_notGivenAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowValidPayload(),
        headers,
        null
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void createWorkflow_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowValidPayload(),
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void createWorkflow_should_returnForbidden_when_givenInvalidPartition() throws Exception {
    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CREATE_WORKFLOW_URL,
        buildCreateWorkflowValidPayload(),
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  /** DELETE WORKFLOW BY ID INTEGRATION TESTS **/

  @Test
  public void deleteWorkflow_should_delete_when_givenValidWorkflowId() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);

    String actualWorkflowName = workflowInfo.get(WORKFLOW_NAME_FIELD);
    ClientResponse deleteResponse = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, actualWorkflowName),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NO_CONTENT, deleteResponse.getStatus());
  }

  @Test
  public void deleteWorkflow_shouldReturnNotFound_when_givenInvalidWorkflowName() throws Exception {
    ClientResponse deleteResponse = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, INVALID_WORKFLOW_NAME),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_NOT_FOUND, deleteResponse.getStatus());
  }

  @Test
  public void deleteWorkflow_should_returnForbidden_when_notGivenAccessToken() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    ClientResponse response = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, getLastCreatedWorkflowName()),
        null,
        headers,
        null
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }

  @Test
  public void deleteWorkflow_should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    ClientResponse response = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, getLastCreatedWorkflowName()),
        null,
        headers,
        client.getNoDataAccessToken()
    );
    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  public void deleteWorkflow_should_returnForbidden_when_givenInvalidPartition() throws Exception {
    String responseBody = createWorkflow();
    Map<String, String> workflowInfo = getWorkflowInfoFromCreateWorkflowResponseBody(responseBody);
    createdWorkflows.add(workflowInfo);
    Map<String, String> headersWithInvalidPartition = new HashMap<>(headers);
    ClientResponse response = client.send(
        HttpMethod.DELETE,
        String.format(GET_WORKFLOW_URL, getLastCreatedWorkflowName()),
        null,
        HTTPClient.overrideHeader(headersWithInvalidPartition, INVALID_PARTITION),
        client.getAccessToken()
    );
    assertTrue(HttpStatus.SC_FORBIDDEN == response.getStatus() || HttpStatus.SC_UNAUTHORIZED == response.getStatus());
  }
}
