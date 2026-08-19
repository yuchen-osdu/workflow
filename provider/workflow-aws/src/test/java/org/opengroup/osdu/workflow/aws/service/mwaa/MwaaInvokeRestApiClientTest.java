/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.workflow.aws.service.mwaa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.mwaa.MwaaClient;
import software.amazon.awssdk.services.mwaa.model.InvokeRestApiRequest;
import software.amazon.awssdk.services.mwaa.model.InvokeRestApiResponse;
import software.amazon.awssdk.services.mwaa.model.RestApiClientException;
import software.amazon.awssdk.services.mwaa.model.RestApiServerException;

@ExtendWith(MockitoExtension.class)
class MwaaInvokeRestApiClientTest {

  private static final String ENVIRONMENT_NAME = "mwaa10-mwaa";
  private static final String ERROR_MESSAGE = "Failed to trigger workflow with id 1 and name mydag";

  @Mock private MwaaClient mwaaClient;

  @Captor private ArgumentCaptor<InvokeRestApiRequest> requestCaptor;

  private MwaaInvokeRestApiClient client;

  @BeforeEach
  void setup() {
    client = new MwaaInvokeRestApiClient(mwaaClient, ENVIRONMENT_NAME);
  }

  private static InvokeRestApiResponse response(int statusCode, Document body) {
    return InvokeRestApiResponse.builder().restApiStatusCode(statusCode).restApiResponse(body).build();
  }

  // --- URL path stripping ---------------------------------------------------------------

  @Test
  void callAirflow_stripsApiV2Prefix_forDagTrigger() {
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenReturn(response(200, Document.fromMap(Map.of("dag_run_id", Document.fromString("run-1")))));

    client.callAirflow("POST", "api/v2/dags/mydag/dagRuns", null, null, ERROR_MESSAGE);

    verify(mwaaClient).invokeRestApi(requestCaptor.capture());
    InvokeRestApiRequest sent = requestCaptor.getValue();
    assertEquals(ENVIRONMENT_NAME, sent.name());
    assertEquals("/dags/mydag/dagRuns", sent.path());
    assertEquals("POST", sent.methodAsString());
  }

  @Test
  void callAirflow_stripsApiV1Prefix_forStatusPoll() {
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenReturn(response(200, Document.fromMap(Map.of("state", Document.fromString("success")))));

    client.callAirflow("GET", "api/v1/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE);

    verify(mwaaClient).invokeRestApi(requestCaptor.capture());
    InvokeRestApiRequest sent = requestCaptor.getValue();
    assertEquals("/dags/mydag/dagRuns/run-1", sent.path());
    assertEquals("GET", sent.methodAsString());
    assertNull(sent.body(), "GET requests must not set a request body");
  }

  @Test
  void callAirflow_stripsApiV2Prefix_forVersionEndpointWithNoTrailingSegments() {
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenReturn(response(200, Document.fromMap(Map.of("version", Document.fromString("3.2.1")))));

    client.callAirflow("GET", "api/v2/version", null, null, ERROR_MESSAGE);

    verify(mwaaClient).invokeRestApi(requestCaptor.capture());
    assertEquals("/version", requestCaptor.getValue().path());
  }

  // --- JSON body -> Document conversion (request direction) ------------------------------

  @Test
  void callAirflow_requestBody_convertsNestedJsonToDocumentPreservingTypes() {
    String jsonBody =
        "{\"conf\":{\"key\":\"value\"},\"dag_run_id\":\"run-1\",\"count\":3,\"ratio\":2.5,"
            + "\"flag\":false,\"items\":[1,true,null,\"txt\"]}";
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(200, Document.fromNull()));

    client.callAirflow("POST", "api/v2/dags/mydag/dagRuns", jsonBody, null, ERROR_MESSAGE);

    verify(mwaaClient).invokeRestApi(requestCaptor.capture());
    Map<String, Document> body = requestCaptor.getValue().body().asMap();

    assertEquals("run-1", body.get("dag_run_id").asString());
    assertEquals("value", body.get("conf").asMap().get("key").asString());
    assertEquals(3, body.get("count").asNumber().intValue());
    assertEquals(2.5, body.get("ratio").asNumber().doubleValue(), 0.0001);
    assertEquals(false, body.get("flag").asBoolean());

    List<Document> items = body.get("items").asList();
    assertEquals(4, items.size());
    assertEquals(1, items.get(0).asNumber().intValue());
    assertEquals(true, items.get(1).asBoolean());
    assertTrue(items.get(2).isNull());
    assertEquals("txt", items.get(3).asString());
  }

  @Test
  void callAirflow_blankRequestBody_doesNotSetDocumentBody() {
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(200, Document.fromNull()));

    client.callAirflow("GET", "api/v2/version", "", null, ERROR_MESSAGE);

    verify(mwaaClient).invokeRestApi(requestCaptor.capture());
    assertNull(requestCaptor.getValue().body());
  }

  // --- Document -> JSON body conversion (response direction) / happy path ----------------

  @Test
  void callAirflow_200Response_parsesIntoClientResponseWithJsonStringBody() throws Exception {
    Document responseBody =
        Document.fromMap(
            Map.of(
                "dag_run_id", Document.fromString("run-1"),
                "logical_date", Document.fromString("2026-01-01T00:00:00Z")));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(200, responseBody));

    ClientResponse result =
        client.callAirflow("POST", "api/v2/dags/mydag/dagRuns", "{}", null, ERROR_MESSAGE);

    assertEquals(HttpStatus.OK, result.getStatus());
    assertEquals(200, result.getStatusCode());
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getStatusMessage());
    assertEquals(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, result.getContentType());

    Map<?, ?> parsedBody = new ObjectMapper().readValue((String) result.getResponseBody(), Map.class);
    assertEquals("run-1", parsedBody.get("dag_run_id"));
    assertEquals("2026-01-01T00:00:00Z", parsedBody.get("logical_date"));
  }

  @Test
  void callAirflow_responseWithNestedMapAndNull_roundTripsToValidJson() throws Exception {
    Document responseBody =
        Document.fromMap(
            Map.of(
                "state", Document.fromString("success"),
                "note", Document.fromNull()));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(200, responseBody));

    ClientResponse result =
        client.callAirflow("GET", "api/v2/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE);

    Map<?, ?> parsed = new ObjectMapper().readValue((String) result.getResponseBody(), Map.class);
    assertEquals("success", parsed.get("state"));
    assertTrue(parsed.containsKey("note"));
    assertNull(parsed.get("note"));
  }

  // --- Document -> JSON numeric field preservation (regression guard) --------------------

  @Test
  void callAirflow_responseWithNumericFields_serializesAsUnquotedJsonNumbers() {
    Document responseBody =
        Document.fromMap(
            Map.of(
                "total_entries", Document.fromNumber(5),
                "try_number", Document.fromNumber(1),
                "dag_run_id", Document.fromString("run-1")));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(200, responseBody));

    ClientResponse result =
        client.callAirflow("GET", "api/v2/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE);

    String rawBody = (String) result.getResponseBody();
    assertTrue(
        rawBody.contains("\"total_entries\":5"),
        "total_entries must serialize as an unquoted JSON number, got: " + rawBody);
    assertTrue(
        rawBody.contains("\"try_number\":1"),
        "try_number must serialize as an unquoted JSON number, got: " + rawBody);
    assertTrue(rawBody.contains("\"dag_run_id\":\"run-1\""), "got: " + rawBody);
  }

  @Test
  void callAirflow_responseWithNestedAndFractionalNumericFields_preservesNumericTypesAtEveryLevel()
      throws Exception {
    Document taskInstance =
        Document.fromMap(
            Map.of(
                "try_number", Document.fromNumber(2),
                "duration", Document.fromNumber(1.5)));
    Document responseBody =
        Document.fromMap(
            Map.of(
                "total_entries", Document.fromNumber(42),
                "task_instances", Document.fromList(List.of(taskInstance)),
                "task_instance", taskInstance));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(200, responseBody));

    ClientResponse result =
        client.callAirflow("GET", "api/v2/dags/mydag/dagRuns/run-1/taskInstances", null, null, ERROR_MESSAGE);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode parsed = mapper.readTree((String) result.getResponseBody());

    assertTrue(parsed.get("total_entries").isNumber(), "total_entries must be a JSON number node");
    assertEquals(42, parsed.get("total_entries").intValue());

    JsonNode nestedInMap = parsed.get("task_instance");
    assertTrue(nestedInMap.get("try_number").isNumber(), "nested map try_number must be numeric");
    assertEquals(2, nestedInMap.get("try_number").intValue());
    assertTrue(nestedInMap.get("duration").isNumber(), "nested map duration must be numeric");
    assertEquals(1.5, nestedInMap.get("duration").doubleValue(), 0.0001);

    JsonNode nestedInList = parsed.get("task_instances").get(0);
    assertTrue(nestedInList.get("try_number").isNumber(), "nested list try_number must be numeric");
    assertEquals(2, nestedInList.get("try_number").intValue());
    assertTrue(nestedInList.get("duration").isNumber(), "nested list duration must be numeric");
    assertEquals(1.5, nestedInList.get("duration").doubleValue(), 0.0001);
  }

  @Test
  void callAirflow_nullRestApiStatusCode_defaultsToOk() {
    InvokeRestApiResponse noStatusCode =
        InvokeRestApiResponse.builder().restApiResponse(Document.fromMap(Map.of())).build();
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(noStatusCode);

    ClientResponse result = client.callAirflow("GET", "api/v2/version", null, null, ERROR_MESSAGE);

    assertEquals(200, result.getStatusCode());
    assertEquals(HttpStatus.OK, result.getStatus());
  }

  @Test
  void callAirflow_201Response_isTreatedAsSuccessNotError() {
    Document responseBody = Document.fromMap(Map.of("dag_run_id", Document.fromString("run-1")));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class))).thenReturn(response(201, responseBody));

    ClientResponse result =
        client.callAirflow("POST", "api/v2/dags/mydag/dagRuns", "{}", null, ERROR_MESSAGE);

    assertEquals(201, result.getStatusCode());
    assertEquals(HttpStatus.CREATED, result.getStatus());
    assertEquals(HttpStatus.CREATED.getReasonPhrase(), result.getStatusMessage());
  }

  @Test
  void callAirflow_204Response_isTreatedAsSuccessNotError() {
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenReturn(response(204, Document.fromNull()));

    ClientResponse result =
        client.callAirflow("DELETE", "api/v2/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE);

    assertEquals(204, result.getStatusCode());
    assertEquals(HttpStatus.NO_CONTENT, result.getStatus());
    assertEquals(HttpStatus.NO_CONTENT.getReasonPhrase(), result.getStatusMessage());
  }

  // --- Non-200 / error handling ------------------------------------------------------------

  @Test
  void callAirflow_non200RestApiStatusCode_throwsAppExceptionWithResponseBodyAsReason() {
    Document errorBody = Document.fromMap(Map.of("detail", Document.fromString("DAG not found")));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenThrow(
            RestApiClientException.builder()
                .restApiStatusCode(404)
                .restApiResponse(errorBody)
                .build());

    AppException ex =
        assertThrows(
            AppException.class,
            () -> client.callAirflow("GET", "api/v2/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE));

    assertEquals(404, ex.getError().getCode());
    assertEquals("{\"detail\":\"DAG not found\"}", ex.getError().getReason());
    assertEquals(ERROR_MESSAGE, ex.getError().getMessage());
  }

  @Test
  void callAirflow_restApiServerException_throwsAppExceptionWithResponseBodyAsReason() {
    Document errorBody =
        Document.fromMap(Map.of("detail", Document.fromString("Airflow internal error")));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenThrow(
            RestApiServerException.builder()
                .restApiStatusCode(503)
                .restApiResponse(errorBody)
                .build());

    AppException ex =
        assertThrows(
            AppException.class,
            () -> client.callAirflow("GET", "api/v2/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE));

    assertEquals(503, ex.getError().getCode());
    assertEquals("{\"detail\":\"Airflow internal error\"}", ex.getError().getReason());
    assertEquals(ERROR_MESSAGE, ex.getError().getMessage());
  }

  @Test
  void callAirflow_restApiClientExceptionWithNumericField_serializesReasonWithUnquotedJsonNumber() {
    Document errorBody =
        Document.fromMap(
            Map.of(
                "detail", Document.fromString("DAG not found"),
                "status", Document.fromNumber(404)));
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenThrow(
            RestApiClientException.builder()
                .restApiStatusCode(404)
                .restApiResponse(errorBody)
                .build());

    AppException ex =
        assertThrows(
            AppException.class,
            () -> client.callAirflow("GET", "api/v2/dags/mydag/dagRuns/run-1", null, null, ERROR_MESSAGE));

    assertTrue(
        ex.getError().getReason().contains("\"status\":404"),
        "numeric field in RestApiClientException body must serialize as an unquoted JSON number, got: "
            + ex.getError().getReason());
  }

  @Test
  void callAirflow_sdkExceptionFromMwaaClient_wrapsAsInternalServerAppException() {
    when(mwaaClient.invokeRestApi(any(InvokeRestApiRequest.class)))
        .thenThrow(SdkClientException.create("No Airflow role granted in IAM."));

    AppException ex =
        assertThrows(
            AppException.class,
            () -> client.callAirflow("POST", "api/v2/dags/mydag/dagRuns", "{}", null, ERROR_MESSAGE));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getError().getCode());
    assertEquals("Error calling MWAA InvokeRestApi", ex.getError().getReason());
    assertEquals(ERROR_MESSAGE, ex.getError().getMessage());
  }
}
