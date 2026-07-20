package org.opengroup.osdu.azure.workflow.framework.operators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.TestBase;

import javax.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.CUSTOM_OPERATOR_URL;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_NAME_KEY;

public abstract class GetCustomOperatorsIntegrationTests extends TestBase {
  private static final List<Integer> INVALID_LIMITS = Arrays.asList(-1, 0, 55);
  private static final String INVALID_CURSOR = "invalid_cursor";
  private static final String RESPONSE_ITEMS_KEY = "items";
  private static final String RESPONSE_CURSOR_KEY = "cursor";
  private static final Integer DEFAULT_LIMIT = 50;

  private static Map<String, JsonObject> testDataOperatorNameToInfo;

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  /*
  @BeforeAll
  public static void initialize() {
    testDataOperatorNameToInfo = new HashMap<>();
    JsonObject operatorsData = TestDataUtil.getAllOperators();
    for(String operatorKey: operatorsData.keySet()) {
      JsonObject operatorData = operatorsData.getAsJsonObject(operatorKey);
      testDataOperatorNameToInfo.put(operatorData.get(CUSTOM_OPERATOR_NAME_KEY).getAsString(),
          operatorData);
    }
  }
  */

  @Test
  @Disabled
  public void should_return_operators_when_valid_request() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL,
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    JsonObject responseData = gson.fromJson(response.getEntity(String.class), JsonObject.class);
    assertTrue(responseData.has(RESPONSE_ITEMS_KEY));
    if(testDataOperatorNameToInfo.size() < DEFAULT_LIMIT) {
      verifyResponseOperatorsDataWithTestData(responseData, testDataOperatorNameToInfo.size());
    } else {
      verifyResponseOperatorsDataWithTestData(responseData, DEFAULT_LIMIT);
    }
  }

  @Test
  @Disabled
  public void should_return_operators_when_valid_request_with_limit() throws Exception {
    final int limit  = 1;
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL + String.format("?limit=%d", limit),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    JsonObject responseData = gson.fromJson(response.getEntity(String.class), JsonObject.class);
    assertTrue(responseData.has(RESPONSE_ITEMS_KEY));
    assertEquals(limit, responseData.getAsJsonArray(RESPONSE_ITEMS_KEY).size());
    verifyResponseOperatorsDataWithTestData(responseData, 1);
  }

  @Test
  @Disabled
  public void should_return_operators_when_valid_request_with_limit_and_cursor() throws Exception {
    final int limit  = 1;
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL + String.format("?limit=%d", limit),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    JsonObject responseData = gson.fromJson(response.getEntity(String.class), JsonObject.class);
    assertTrue(responseData.has(RESPONSE_ITEMS_KEY));
    assertTrue(responseData.has(RESPONSE_CURSOR_KEY));
    assertEquals(limit, responseData.getAsJsonArray(RESPONSE_ITEMS_KEY).size());
    verifyResponseOperatorsDataWithTestData(responseData, limit);

    JsonArray operatorsData = responseData.getAsJsonArray(RESPONSE_ITEMS_KEY);
    String cursor = responseData.get(RESPONSE_CURSOR_KEY).getAsString();
    response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL + String.format("?limit=%d&cursor=%s", limit, cursor),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    responseData = gson.fromJson(response.getEntity(String.class), JsonObject.class);
    assertTrue(responseData.has(RESPONSE_ITEMS_KEY));
    assertEquals(limit, responseData.getAsJsonArray(RESPONSE_ITEMS_KEY).size());
    verifyResponseOperatorsDataWithTestData(responseData, limit);
    operatorsData.addAll(responseData.getAsJsonArray(RESPONSE_ITEMS_KEY));
    checkForDuplicateItems(operatorsData);
  }

  @Test
  @Disabled
  public void should_returnBadRequest_when_request_with_invalid_limit() throws Exception {
    for(Integer invalidLimit: INVALID_LIMITS) {
      ClientResponse response = client.send(
          HttpMethod.GET,
          CUSTOM_OPERATOR_URL + String.format("?limit=%d", invalidLimit),
          null,
          headers,
          client.getAccessToken()
      );
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  @Disabled
  public void should_returnBadRequest_when_request_with_invalid_cursor() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL + String.format("?cursor=%s", INVALID_CURSOR),
        null,
        headers,
        client.getAccessToken()
    );
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }


  @Test
  @Disabled
  public void should_returnBadRequest_when_request_with_invalid_limit_and_cursor()
      throws Exception {
    for(Integer invalidLimit: INVALID_LIMITS) {
      ClientResponse response = client.send(
          HttpMethod.GET,
          CUSTOM_OPERATOR_URL + String.format("?limit=%d&cursor=%s", invalidLimit, INVALID_CURSOR),
          null,
          headers,
          client.getAccessToken()
      );
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_notGivenAccessToken() {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL,
        null,
        headers,
        null
    );

    assertTrue(response.getStatus()== HttpStatus.SC_FORBIDDEN || response.getStatus()== HttpStatus.SC_UNAUTHORIZED) ;
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL,
        null,
        headers,
        client.getNoDataAccessToken()
    );

    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        CUSTOM_OPERATOR_URL,
        null,
        HTTPClient.overrideHeader(headers, "invalid-partition"),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }

  private void verifyResponseOperatorsDataWithTestData(JsonObject responseData,
                                                       int expectedOperatorsCountToMatch) {
    int matchedOperatorsCount = 0;
    for(JsonElement responseOperator: responseData.getAsJsonArray(RESPONSE_ITEMS_KEY)) {
      JsonObject responseOperatorData = responseOperator.getAsJsonObject();
      String responseOperatorName = responseOperatorData.get(CUSTOM_OPERATOR_NAME_KEY)
          .getAsString();
      if(testDataOperatorNameToInfo.containsKey(responseOperatorName)) {
        JsonObject testOperatorData = testDataOperatorNameToInfo.get(responseOperatorName);
        assertEquals(testOperatorData, responseOperatorData);
        matchedOperatorsCount++;
      }
    }
    assertEquals(expectedOperatorsCountToMatch, matchedOperatorsCount);
    checkForDuplicateItems(responseData.getAsJsonArray(RESPONSE_ITEMS_KEY));
  }

  private void checkForDuplicateItems(JsonArray items) {
    Set<String> operatorNames = new HashSet<>();
    for(JsonElement operator: items) {
      String operatorName = operator.getAsJsonObject().get(CUSTOM_OPERATOR_NAME_KEY).getAsString();
      assertFalse(operatorNames.contains(operatorName));
      operatorNames.add(operatorName);
    }
  }
}
