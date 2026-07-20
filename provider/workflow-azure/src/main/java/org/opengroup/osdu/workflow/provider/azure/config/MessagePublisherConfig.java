package org.opengroup.osdu.workflow.provider.azure.config;

import org.opengroup.osdu.azure.publisherFacade.models.PubSubAttributesBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagePublisherConfig {

  @Bean
  public PubSubAttributesBuilder getPubSub() {
    return PubSubAttributesBuilder.builder().build();
  }

}
