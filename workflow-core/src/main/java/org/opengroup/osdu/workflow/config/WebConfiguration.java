package org.opengroup.osdu.workflow.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

/**
 * Spring Framework 7 removed trailing slash path matching; this filter keeps "/path/" and "/path"
 * mapped to the same handler, ahead of security and request logging filters.
 */
@Configuration
public class WebConfiguration {

  @Bean
  public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
    FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(
        UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
    return registration;
  }
}
