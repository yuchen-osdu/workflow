package org.opengroup.osdu.azure.workflow.framework.operators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.TestBase;
import org.opengroup.osdu.azure.workflow.framework.util.TestDataUtil;

import javax.ws.rs.HttpMethod;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.CUSTOM_OPERATOR_BY_ID_URL;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestOperatorNames.SIMPLE_CUSTOM_OPERATOR;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_ID_KEY;

public abstract class GetCustomOperatorByIdIntegrationTests extends TestBase {
  private static final String INVALID_OPERATOR_ID = "invalid_operator_id";

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  @Test
  @Disabled
  public void should_return_operator_when_request_with_valid_id() throws Exception {
    JsonObject testOperatorData = TestDataUtil.getOperator(SIMPLE_CUSTOM_OPERATOR);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CUSTOM_OPERATOR_BY_ID_URL,
            testOperatorData.get(CUSTOM_OPERATOR_ID_KEY).getAsString()),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    JsonObject responseData = gson.fromJson(response.getEntity(String.class), JsonObject.class);
    assertEquals(testOperatorData, responseData);
  }

  @Test
  @Disabled
  public void should_return_notFound_when_request_with_invalid_id() throws Exception {
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CUSTOM_OPERATOR_BY_ID_URL, INVALID_OPERATOR_ID),
        null,
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_notGivenAccessToken() {
    JsonObject testOperatorData = TestDataUtil.getOperator(SIMPLE_CUSTOM_OPERATOR);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CUSTOM_OPERATOR_BY_ID_URL,
            testOperatorData.get(CUSTOM_OPERATOR_ID_KEY).getAsString()),
        null,
        headers,
        null
    );

    assertTrue(response.getStatus()== HttpStatus.SC_FORBIDDEN || response.getStatus()== HttpStatus.SC_UNAUTHORIZED) ;
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    JsonObject testOperatorData = TestDataUtil.getOperator(SIMPLE_CUSTOM_OPERATOR);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CUSTOM_OPERATOR_BY_ID_URL,
            testOperatorData.get(CUSTOM_OPERATOR_ID_KEY).getAsString()),
        null,
        headers,
        client.getNoDataAccessToken()
    );

    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    JsonObject testOperatorData = TestDataUtil.getOperator(SIMPLE_CUSTOM_OPERATOR);
    ClientResponse response = client.send(
        HttpMethod.GET,
        String.format(CUSTOM_OPERATOR_BY_ID_URL,
            testOperatorData.get(CUSTOM_OPERATOR_ID_KEY).getAsString()),
        null,
        HTTPClient.overrideHeader(headers, "invalid-partition"),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }
}
