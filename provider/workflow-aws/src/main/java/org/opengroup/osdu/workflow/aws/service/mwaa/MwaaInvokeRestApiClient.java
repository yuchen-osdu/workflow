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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAirflowApiClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.mwaa.MwaaClient;
import software.amazon.awssdk.services.mwaa.model.InvokeRestApiRequest;
import software.amazon.awssdk.services.mwaa.model.InvokeRestApiResponse;
import software.amazon.awssdk.services.mwaa.model.RestApiClientException;
import software.amazon.awssdk.services.mwaa.model.RestApiServerException;

/**
 * IAirflowApiClient implementation that talks to MWAA's IAM SigV4-signed InvokeRestApi
 * control-plane action instead of calling the Airflow REST API directly. InvokeRestApi is
 * version-agnostic: MWAA proxies the call to api/v1 (Airflow 2) or api/v2 (Airflow 3) internally
 * based on the environment's Airflow version, so this client strips the api/v1/ or api/v2/
 * prefix that the engine implementations build into apiEndpoint and calls MWAA with the bare
 * Airflow REST path (e.g. /dags/{id}/dagRuns), as confirmed against a real MWAA environment.
 *
 * <p>No username/password/JWT is used: authentication is IAM SigV4, signed by the AWS SDK from
 * the caller's credentials (IRSA pod role in production).
 *
 * <p>InvokeRestApi itself is version-agnostic, but the AWS wiring is not: AwsMwaaEngineConfig
 * binds this client directly to the Airflow 3 engine only, with no dual Airflow 2/3 mode and no
 * per-run version routing.
 */
@Slf4j
public class MwaaInvokeRestApiClient implements IAirflowApiClient {

  private static final String API_V1_PATH_PREFIX = "api/v1/";
  private static final String API_V2_PATH_PREFIX = "api/v2/";

  private final MwaaClient mwaaClient;
  private final String environmentName;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public MwaaInvokeRestApiClient(MwaaClient mwaaClient, String environmentName) {
    this.mwaaClient = mwaaClient;
    this.environmentName = environmentName;
    log.info("Initialized MWAA InvokeRestApi client for environment {}.", environmentName);
  }

  @Override
  public ClientResponse callAirflow(
      String httpMethod,
      String apiEndpoint,
      String body,
      WorkflowEngineRequest rq,
      String errorMessage) {
    String path = toMwaaPath(apiEndpoint);
    log.info(
        "Calling MWAA InvokeRestApi environment {} path {} with method {}",
        environmentName,
        path,
        httpMethod);

    InvokeRestApiRequest.Builder requestBuilder =
        InvokeRestApiRequest.builder().name(environmentName).path(path).method(httpMethod);
    if (body != null && !body.isBlank()) {
      requestBuilder.body(toDocument(parseJson(body)));
    }

    InvokeRestApiResponse response;
    try {
      response = mwaaClient.invokeRestApi(requestBuilder.build());
      // NOTE: catch the RestApiClientException/RestApiServerException subtypes before the
      // generic SdkException below -- unlike a typical SDK call, invokeRestApi THROWS these
      // when Airflow itself returns a 4xx/5xx, instead of returning them in the response, so
      // the real Airflow status/body must be extracted here or it would otherwise be collapsed
      // into a generic 500 by the SdkException fallback.
    } catch (RestApiClientException e) {
      int status =
          e.restApiStatusCode() == null ? HttpStatus.BAD_REQUEST.value() : e.restApiStatusCode();
      throw new AppException(status, toJson(e.restApiResponse()), errorMessage, e);
    } catch (RestApiServerException e) {
      int status =
          e.restApiStatusCode() == null
              ? HttpStatus.INTERNAL_SERVER_ERROR.value()
              : e.restApiStatusCode();
      throw new AppException(status, toJson(e.restApiResponse()), errorMessage, e);
    } catch (SdkException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Error calling MWAA InvokeRestApi",
          errorMessage,
          e);
    }

    int status =
        response.restApiStatusCode() == null
            ? HttpStatus.OK.value()
            : response.restApiStatusCode();
    String responseBody = toJson(response.restApiResponse());
    log.info("Received MWAA InvokeRestApi response status: {}.", status);

    if (status < HttpStatus.OK.value() || status >= HttpStatus.MULTIPLE_CHOICES.value()) {
      throw new AppException(status, responseBody, errorMessage);
    }

    // HttpStatus.resolve() is null-safe, unlike JwtAuthAirflowApiClient's HttpStatus.valueOf():
    // MWAA can proxy uncommon 2xx codes that don't map to a Spring HttpStatus constant, and the
    // success path here must never throw for those, so we fall back to HttpStatus.OK.
    HttpStatus resolvedStatus = HttpStatus.resolve(status);
    HttpStatus responseStatus = resolvedStatus != null ? resolvedStatus : HttpStatus.OK;

    return ClientResponse.builder()
        .status(responseStatus)
        .statusCode(status)
        .statusMessage(responseStatus.getReasonPhrase())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .responseBody(responseBody)
        .build();
  }

  /**
   * MWAA's InvokeRestApi routes to the correct Airflow REST API version internally, so the
   * api/v1 or api/v2 prefix that the engine implementations bake into apiEndpoint must be
   * stripped before calling MWAA (e.g. Path='/dags/my_dag/dagRuns', not
   * Path='/api/v2/dags/my_dag/dagRuns').
   */
  private static String toMwaaPath(String apiEndpoint) {
    String trimmed = apiEndpoint.startsWith("/") ? apiEndpoint.substring(1) : apiEndpoint;
    if (trimmed.startsWith(API_V1_PATH_PREFIX)) {
      trimmed = trimmed.substring(API_V1_PATH_PREFIX.length());
    } else if (trimmed.startsWith(API_V2_PATH_PREFIX)) {
      trimmed = trimmed.substring(API_V2_PATH_PREFIX.length());
    }
    return "/" + trimmed;
  }

  private Object parseJson(String json) {
    try {
      return objectMapper.readValue(json, Object.class);
    } catch (JsonProcessingException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Unable to build MWAA request body",
          "Unable to parse request body as JSON: " + e.getMessage(),
          e);
    }
  }

  private String toJson(Document document) {
    if (document == null) {
      return "";
    }
    try {
      return objectMapper.writeValueAsString(toJsonNode(document));
    } catch (JsonProcessingException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Unable to parse MWAA response body",
          "Unable to serialize MWAA response as JSON: " + e.getMessage(),
          e);
    }
  }

  /**
   * Recursively converts an AWS SDK Document into a Jackson JsonNode tree, preserving each
   * value's original type. Document.unwrap() must NOT be used for this: NumberDocument#unwrap()
   * returns SdkNumber#stringValue() (a String), which would silently stringify numeric Airflow
   * response fields (e.g. total_entries, try_number) into quoted JSON strings instead of real
   * JSON numbers, recursively for any numbers nested in maps/lists.
   */
  private static JsonNode toJsonNode(Document document) {
    if (document.isNull()) {
      return JsonNodeFactory.instance.nullNode();
    }
    if (document.isBoolean()) {
      return JsonNodeFactory.instance.booleanNode(document.asBoolean());
    }
    if (document.isNumber()) {
      return JsonNodeFactory.instance.numberNode(document.asNumber().bigDecimalValue());
    }
    if (document.isString()) {
      return JsonNodeFactory.instance.textNode(document.asString());
    }
    if (document.isMap()) {
      ObjectNode node = JsonNodeFactory.instance.objectNode();
      document.asMap().forEach((key, value) -> node.set(key, toJsonNode(value)));
      return node;
    }
    if (document.isList()) {
      List<Document> children = document.asList();
      ArrayNode node = JsonNodeFactory.instance.arrayNode(children.size());
      children.forEach(value -> node.add(toJsonNode(value)));
      return node;
    }
    return JsonNodeFactory.instance.textNode(document.unwrap().toString());
  }

  /**
   * Recursively converts a Jackson-parsed generic JSON value (Map/List/String/Number/Boolean/
   * null) into the AWS SDK's Document type required by InvokeRestApiRequest.body.
   */
  // NOTE: classic instanceof + cast, not pattern-matching `instanceof` -- checkstyle 3.1.0's
  // bundled ANTLR grammar can't parse binding patterns and would break the build.
  private static Document toDocument(Object value) {
    if (value == null) {
      return Document.fromNull();
    }
    if (value instanceof Map) {
      Map<?, ?> mapValue = (Map<?, ?>) value;
      Map<String, Document> children = new LinkedHashMap<>();
      mapValue.forEach((key, val) -> children.put(String.valueOf(key), toDocument(val)));
      return Document.fromMap(children);
    }
    if (value instanceof List) {
      List<?> listValue = (List<?>) value;
      List<Document> children = new ArrayList<>(listValue.size());
      for (Object item : listValue) {
        children.add(toDocument(item));
      }
      return Document.fromList(children);
    }
    if (value instanceof String) {
      String stringValue = (String) value;
      return Document.fromString(stringValue);
    }
    if (value instanceof Boolean) {
      Boolean booleanValue = (Boolean) value;
      return Document.fromBoolean(booleanValue);
    }
    if (value instanceof Number) {
      Number numberValue = (Number) value;
      return Document.fromNumber(numberValue.toString());
    }
    return Document.fromString(value.toString());
  }
}
