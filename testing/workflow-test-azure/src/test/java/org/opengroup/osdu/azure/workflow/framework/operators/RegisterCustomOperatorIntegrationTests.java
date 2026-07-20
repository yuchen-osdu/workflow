package org.opengroup.osdu.azure.workflow.framework.operators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder;
import org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorUtil;
import org.opengroup.osdu.azure.workflow.framework.util.HTTPClient;
import org.opengroup.osdu.azure.workflow.framework.util.TestBase;
import org.opengroup.osdu.azure.workflow.framework.util.TestDataUtil;
import org.opengroup.osdu.azure.workflow.framework.util.TestResourceProvider;

import javax.ws.rs.HttpMethod;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestConstants.CUSTOM_OPERATOR_URL;
import static org.opengroup.osdu.azure.workflow.framework.consts.TestOperatorNames.SIMPLE_CUSTOM_OPERATOR;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_CLASS_NAME_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_CREATED_AT_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_DESCRIPTION_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_ID_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_NAME_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.CUSTOM_OPERATOR_PROPERTIES_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.PROPERTY_DESCRIPTION_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.PROPERTY_MANDATORY_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.PROPERTY_NAME_KEY;
import static org.opengroup.osdu.azure.workflow.framework.util.CustomOperatorTestsBuilder.SAMPLE_DESCRIPTION;

public abstract class RegisterCustomOperatorIntegrationTests extends TestBase {
  private static final String SIMPLE_CUSTOM_OPERATOR_FILE = "simple_custom_operator.py";
  private static final String SIMPLE_CUSTOM_OPERATOR_PROPERTY_NAME = "name";
  private static final String SIMPLE_CUSTOM_OPERATOR_PROPERTY_DESCRIPTION =
      "Name to be used by the operator to print";
  private static final Boolean SIMPLE_CUSTOM_OPERATOR_PROPERTY_MANDATORY = true;
  public static final String CUSTOM_OPERATOR_CONFLICT_MESSAGE =
      "ResourceConflictException: Custom operator with name %s and id %s already exists";

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  @Test
  @Disabled
  public void should_return_success_when_given_valid_request() {
    JsonObject responseData = TestDataUtil.getOperator(SIMPLE_CUSTOM_OPERATOR);

    assertTrue(isNotBlank(responseData.get(CUSTOM_OPERATOR_ID_KEY).getAsString()));
    assertTrue(isNotBlank(responseData.get(CUSTOM_OPERATOR_NAME_KEY).getAsString()));
    assertTrue(responseData.has(CUSTOM_OPERATOR_CREATED_AT_KEY));
    assertEquals("HelloOperator", responseData.get(CUSTOM_OPERATOR_CLASS_NAME_KEY).getAsString());
    assertEquals(SAMPLE_DESCRIPTION, responseData.get(CUSTOM_OPERATOR_DESCRIPTION_KEY)
        .getAsString());
    assertEquals(1, responseData.get(CUSTOM_OPERATOR_PROPERTIES_KEY).getAsJsonArray().size());

    JsonObject property = responseData.get(CUSTOM_OPERATOR_PROPERTIES_KEY).getAsJsonArray().get(0).getAsJsonObject();
    assertEquals(SIMPLE_CUSTOM_OPERATOR_PROPERTY_NAME, property.get(PROPERTY_NAME_KEY).getAsString());
    assertEquals(SIMPLE_CUSTOM_OPERATOR_PROPERTY_DESCRIPTION, property.get(PROPERTY_DESCRIPTION_KEY).getAsString());
    assertEquals(SIMPLE_CUSTOM_OPERATOR_PROPERTY_MANDATORY, property.get(PROPERTY_MANDATORY_KEY).getAsBoolean());
  }

  @Test
  @Disabled
  public void should_return_badRequest_given_invalid_request() throws Exception {
    String operatorContent = TestResourceProvider
        .getOperatorFileContent(SIMPLE_CUSTOM_OPERATOR_FILE);
    Map<String, Object> invalidPayload =
        CustomOperatorTestsBuilder.buildInvalidRegisterCustomOperatorPayload(
            CustomOperatorUtil.getUniqueOperatorName(SIMPLE_CUSTOM_OPERATOR), "SampleClass",
            operatorContent);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CUSTOM_OPERATOR_URL,
        gson.toJson(invalidPayload),
        client.getCommonHeader(),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  @Disabled
  public void should_return_conflict_given_valid_request_with_existing_operator()
      throws Exception {
    JsonObject testOperatorData = TestDataUtil.getOperator(SIMPLE_CUSTOM_OPERATOR);
    String operatorContent = TestResourceProvider
        .getOperatorFileContent(SIMPLE_CUSTOM_OPERATOR_FILE);
    Map<String, Object> payload =
        CustomOperatorTestsBuilder.buildRegisterCustomOperatorPayload(
            testOperatorData.get(CUSTOM_OPERATOR_NAME_KEY).getAsString(),
            testOperatorData.get(CUSTOM_OPERATOR_CLASS_NAME_KEY).getAsString(),
            operatorContent);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CUSTOM_OPERATOR_URL,
        gson.toJson(payload),
        client.getCommonHeader(),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_CONFLICT, response.getStatus());

    JsonObject duplicateResponseBody = gson.fromJson(response.getEntity(String.class),
        JsonObject.class);
    assertEquals(testOperatorData.get("id").getAsString(),
        duplicateResponseBody.get("conflictId").getAsString());
    assertEquals(String.format(CUSTOM_OPERATOR_CONFLICT_MESSAGE,
        testOperatorData.get("name").getAsString(),
        testOperatorData.get("id").getAsString()),
        duplicateResponseBody.get("message").getAsString());
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_notGivenAccessToken() throws Exception {
    String operatorContent = TestResourceProvider
        .getOperatorFileContent(SIMPLE_CUSTOM_OPERATOR_FILE);
    Map<String, Object> payload =
        CustomOperatorTestsBuilder.buildRegisterCustomOperatorPayload(
            CustomOperatorUtil.getUniqueOperatorName(SIMPLE_CUSTOM_OPERATOR), "SampleClass",
            operatorContent);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CUSTOM_OPERATOR_URL,
        gson.toJson(payload),
        headers,
        null
    );

    assertTrue(response.getStatus()== HttpStatus.SC_FORBIDDEN || response.getStatus()== HttpStatus.SC_UNAUTHORIZED) ;
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_givenNoDataAccessToken() throws Exception {
    String operatorContent = TestResourceProvider
        .getOperatorFileContent(SIMPLE_CUSTOM_OPERATOR_FILE);
    Map<String, Object> payload =
        CustomOperatorTestsBuilder.buildRegisterCustomOperatorPayload(
            CustomOperatorUtil.getUniqueOperatorName(SIMPLE_CUSTOM_OPERATOR), "SampleClass",
            operatorContent);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CUSTOM_OPERATOR_URL,
        gson.toJson(payload),
        headers,
        client.getNoDataAccessToken()
    );

    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  @Disabled
  public void should_returnUnauthorized_when_givenInvalidPartition() throws Exception {
    String operatorContent = TestResourceProvider
        .getOperatorFileContent(SIMPLE_CUSTOM_OPERATOR_FILE);
    Map<String, Object> payload =
        CustomOperatorTestsBuilder.buildRegisterCustomOperatorPayload(
            CustomOperatorUtil.getUniqueOperatorName(SIMPLE_CUSTOM_OPERATOR), "SampleClass",
            operatorContent);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CUSTOM_OPERATOR_URL,
        gson.toJson(payload),
        HTTPClient.overrideHeader(headers, "invalid-partition"),
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  public void should_returnForbidden_when_givenCustomOperatorContentWithIgnoreCustomOperatorContentAsTrue() throws Exception {
    String operatorContent = TestResourceProvider
        .getOperatorFileContent(SIMPLE_CUSTOM_OPERATOR_FILE);
    Map<String, Object> payload =
        CustomOperatorTestsBuilder.buildRegisterCustomOperatorPayload(
            CustomOperatorUtil.getUniqueOperatorName(SIMPLE_CUSTOM_OPERATOR), "SampleClass",
            operatorContent);
    ClientResponse response = client.send(
        HttpMethod.POST,
        CUSTOM_OPERATOR_URL,
        gson.toJson(payload),
        headers,
        client.getAccessToken()
    );

    assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
  }
}
