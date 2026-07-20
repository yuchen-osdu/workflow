package org.opengroup.osdu.workflow.provider.azure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomObjectMapperConfig {

  //You can explicitly refer this bean later
  @Bean("WorkflowObjectMapper")
  public ObjectMapper getCustomObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
  }
}
