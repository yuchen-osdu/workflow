package org.opengroup.osdu.workflow.config;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.status.Message;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class GSMConfiguration {

  @ConditionalOnMissingBean
  @Bean
  public IEventPublisher getMockPublisher() {
    return new IEventPublisher() {
      @Override
      public void publish(Message[] messages, Map<String, String> map) throws CoreException {
        log.warn("This is not a real message publisher. Please introduce your own implementation of 'IEventPublisher.class'!");
      }
    };
  }
}
