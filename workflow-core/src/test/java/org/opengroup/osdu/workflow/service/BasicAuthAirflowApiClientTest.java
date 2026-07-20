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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.config.AirflowConfig;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BasicAuthAirflowApiClientTest {

  private static final String TEST_API_ENDPOINT = "test/endpoint";
  private static final String TEST_HTTP_METHOD = "POST";
  private static final String TEST_BODY = "{\"foo\": \"bar\"}";
  private static final String TEST_ERROR_MESSAGE = "Error occurred";
  private static final int HTTP_OK = 200;
  private static final int HTTP_NOT_OK = 400;
  private static final String RESPONSE_OK_BODY = "{\"result\": \"ok\"}";
  private static final String RESPONSE_ERROR_BODY = "{\"error\": \"bad\"}";
  private static final String REASON_PHRASE = "OK";
  private static final String ERROR_CALLING_AIRFLOW = "Error calling airflow";

  @Mock private Client restClient;

  @Mock private WebResource webResource;

  @Mock private AirflowConfig airflowConfig;

  @Mock private com.sun.jersey.api.client.ClientResponse apiClientResponse;

  @Mock private com.sun.jersey.api.client.WebResource.Builder webResourceBuilder;

  @Mock private WorkflowEngineRequest workflowEngineRequest;

  @InjectMocks private BasicAuthAirflowApiClient apiClient;

  @Test
  void should_ReturnClientResponse_when_AirflowReturnsStatusOK() {
    when(restClient.resource(anyString())).thenReturn(webResource);
    when(webResource.type(MediaType.APPLICATION_JSON)).thenReturn(webResourceBuilder);
    when(webResourceBuilder.header(anyString(), any())).thenReturn(webResourceBuilder);
    when(webResourceBuilder.method(
            anyString(), eq(com.sun.jersey.api.client.ClientResponse.class), eq(TEST_BODY)))
        .thenReturn(apiClientResponse);

    when(apiClientResponse.getStatus()).thenReturn(HTTP_OK);
    when(apiClientResponse.getEntity(String.class)).thenReturn(RESPONSE_OK_BODY);
    when(apiClientResponse.getType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
    when(apiClientResponse.getStatusInfo())
        .thenReturn(
            new Response.StatusType() {
              @Override
              public int getStatusCode() {
                return 0;
              }

              @Override
              public Response.Status.Family getFamily() {
                return null;
              }

              @Override
              public String getReasonPhrase() {
                return REASON_PHRASE;
              }
            });

    ClientResponse result =
        apiClient.callAirflow(
            TEST_HTTP_METHOD,
            TEST_API_ENDPOINT,
            TEST_BODY,
            workflowEngineRequest,
            TEST_ERROR_MESSAGE);

    assertEquals(RESPONSE_OK_BODY, result.getResponseBody());
    assertEquals(HTTP_OK, result.getStatusCode());
    assertEquals(REASON_PHRASE, result.getStatusMessage());
    assertEquals(
        javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE.toString(), result.getContentType());
  }

  @Test
  void should_ThrowAppException_when_AirflowReturnsNotOkStatus() {
    when(restClient.resource(anyString())).thenReturn(webResource);
    when(webResource.type(MediaType.APPLICATION_JSON)).thenReturn(webResourceBuilder);
    when(webResourceBuilder.header(anyString(), any())).thenReturn(webResourceBuilder);
    when(webResourceBuilder.method(
            anyString(), eq(com.sun.jersey.api.client.ClientResponse.class), eq(TEST_BODY)))
        .thenReturn(apiClientResponse);

    when(apiClientResponse.getStatus()).thenReturn(HTTP_NOT_OK);
    when(apiClientResponse.getEntity(String.class)).thenReturn(RESPONSE_ERROR_BODY);

    AppException ex =
        assertThrows(
            AppException.class,
            () ->
                apiClient.callAirflow(
                    TEST_HTTP_METHOD,
                    TEST_API_ENDPOINT,
                    TEST_BODY,
                    workflowEngineRequest,
                    TEST_ERROR_MESSAGE));

    assertEquals(HTTP_NOT_OK, ex.getError().getCode());
    assertEquals(RESPONSE_ERROR_BODY, ex.getError().getReason());
    assertEquals(TEST_ERROR_MESSAGE, ex.getError().getMessage());
  }

  @Test
  void should_ThrowAppException_when_ClientThrowsUniformInterfaceException() {
    when(restClient.resource(anyString())).thenReturn(webResource);
    when(webResource.type(MediaType.APPLICATION_JSON)).thenReturn(webResourceBuilder);
    when(webResourceBuilder.header(anyString(), any())).thenReturn(webResourceBuilder);
    com.sun.jersey.api.client.ClientResponse stub =
        mock(com.sun.jersey.api.client.ClientResponse.class);
    UniformInterfaceException ex1 = new UniformInterfaceException(stub);
    when(webResourceBuilder.method(
            anyString(), eq(com.sun.jersey.api.client.ClientResponse.class), eq(TEST_BODY)))
        .thenThrow(ex1);

    AppException ex =
        assertThrows(
            AppException.class,
            () ->
                apiClient.callAirflow(
                    TEST_HTTP_METHOD,
                    TEST_API_ENDPOINT,
                    TEST_BODY,
                    workflowEngineRequest,
                    TEST_ERROR_MESSAGE));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getError().getCode());
    assertEquals(ERROR_CALLING_AIRFLOW, ex.getError().getReason());
  }

  @Test
  void should_ThrowAppException_when_ClientThrowsClientHandlerException() {
    when(restClient.resource(anyString())).thenReturn(webResource);
    when(webResource.type(MediaType.APPLICATION_JSON)).thenReturn(webResourceBuilder);
    when(webResourceBuilder.header(anyString(), any())).thenReturn(webResourceBuilder);
    when(webResourceBuilder.method(
            anyString(), eq(com.sun.jersey.api.client.ClientResponse.class), eq(TEST_BODY)))
        .thenThrow(new ClientHandlerException());

    AppException ex =
        assertThrows(
            AppException.class,
            () ->
                apiClient.callAirflow(
                    TEST_HTTP_METHOD,
                    TEST_API_ENDPOINT,
                    TEST_BODY,
                    workflowEngineRequest,
                    TEST_ERROR_MESSAGE));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getError().getCode());
    assertEquals(ERROR_CALLING_AIRFLOW, ex.getError().getReason());
  }
}
