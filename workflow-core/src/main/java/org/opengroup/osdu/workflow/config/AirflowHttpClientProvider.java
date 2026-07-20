package org.opengroup.osdu.workflow.config;

import com.sun.jersey.api.client.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AirflowHttpClientProvider {
  @Bean
  public Client createClient() {
    return Client.create();
  }
}
