package org.opengroup.osdu.azure.workflow.framework.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomOperatorTestsBuilder {
  public static final String SAMPLE_DESCRIPTION = "This is a custom operator";
  public static final String SAMPLE_PROPERTY_NAME = "sample_property";
  public static final String SAMPLE_PROPERTY_DESCRIPTION = "This is a sample property";
  public static final Boolean SAMPLE_PROPERTY_MANDATORY = true;

  // Custom Operator Keys
  public static final String CUSTOM_OPERATOR_ID_KEY = "id";
  public static final String CUSTOM_OPERATOR_NAME_KEY = "name";
  public static final String CUSTOM_OPERATOR_CLASS_NAME_KEY = "className";
  public static final String CUSTOM_OPERATOR_DESCRIPTION_KEY = "description";
  public static final String CUSTOM_OPERATOR_CREATED_AT_KEY = "createdAt";
  public static final String CUSTOM_OPERATOR_PROPERTIES_KEY = "properties";

  // Custom Operator Property Keys
  public static final String PROPERTY_NAME_KEY = "name";
  public static final String PROPERTY_DESCRIPTION_KEY = "description";
  public static final String PROPERTY_MANDATORY_KEY = "mandatory";


  public static Map<String, Object> buildRegisterCustomOperatorPayload(
      String name, String className, String content) {
    return buildRegisterCustomOperatorPayload(name, className, content,
        getCustomOperatorProperties());
  }

  public static Map<String, Object> buildRegisterCustomOperatorPayload(
      String name, String className, String content, List<Map<String, Object>> properties) {
    Map<String, Object> payload = new HashMap<>();

    payload.put("name", name);
    payload.put("className", className);
    payload.put("description", SAMPLE_DESCRIPTION);
    payload.put("content", content);
    payload.put("properties", properties);

    return payload;
  }

  public static Map<String, Object> buildInvalidRegisterCustomOperatorPayload(
      String name, String className, String content) {
    Map<String, Object> payload = buildRegisterCustomOperatorPayload(name, className, content);
    payload.remove("content");
    return payload;
  }

  private static List<Map<String, Object>> getCustomOperatorProperties() {
    List<Map<String, Object>> properties = new ArrayList<>();

    Map<String, Object> property = new HashMap<>();
    property.put("name", SAMPLE_PROPERTY_NAME);
    property.put("description", SAMPLE_PROPERTY_DESCRIPTION);
    property.put("mandatory", SAMPLE_PROPERTY_MANDATORY);

    properties.add(property);
    return properties;
  }
}
