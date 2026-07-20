package org.opengroup.osdu.workflow.provider.azure.security;
import com.azure.spring.cloud.autoconfigure.implementation.aad.filter.AadAppRoleStatelessAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(value = "azure.istio.auth.enabled", havingValue = "false", matchIfMissing = false)
public class AadSecurityConfig  {
  public static final String[] AUTH_ALLOWLIST = {"/", "/index.html",
      "/api-docs.yaml",
      "/api-docs/swagger-config",
      "/api-docs/**",
      "/swagger",
      "/swagger-ui.html",
      "/swagger-ui/**",
  };
  @Autowired
  private AadAppRoleStatelessAuthenticationFilter aadAppRoleStatelessAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement((sess) -> sess.sessionCreationPolicy(SessionCreationPolicy.NEVER))
        .authorizeHttpRequests(request -> request.requestMatchers(AUTH_ALLOWLIST).permitAll())
        .addFilterBefore(aadAppRoleStatelessAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
