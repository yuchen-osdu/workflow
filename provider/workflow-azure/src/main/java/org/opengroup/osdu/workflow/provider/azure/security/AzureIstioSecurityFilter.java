package org.opengroup.osdu.workflow.provider.azure.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.filter.UserPrincipal;
import com.nimbusds.jwt.JWTClaimsSet;
import net.minidev.json.JSONArray;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Component
@ConditionalOnProperty(value = "azure.istio.auth.enabled", havingValue = "true", matchIfMissing = true)
public class AzureIstioSecurityFilter extends OncePerRequestFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AzureIstioSecurityFilter.class);

  private static final String X_ISTIO_CLAIMS_PAYLOAD = "x-payload";
  private static final JSONArray DEFAULT_ROLE_CLAIM = new JSONArray().appendElement("USER");
  private static final String ROLE_PREFIX = "ROLE_";

  /**
   * Filter logic.
   * @param servletRequest Request object.
   * @param servletResponse Response object.
   * @param filterChain Filter Chain object.
   * @throws IOException
   * @throws ServletException
   */
  @Override
  protected void doFilterInternal(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final FilterChain filterChain) throws ServletException, IOException {
    final String istioPayload = servletRequest.getHeader(X_ISTIO_CLAIMS_PAYLOAD);

    LOGGER.debug("Received headers list: {}", Collections.list(servletRequest.getHeaderNames()));

    try {
      if (hasText(istioPayload)) {
        JWTClaimsSet claimsSet = JWTClaimsSet.parse(new String(Base64.getDecoder().decode(istioPayload)));

        final JSONArray roles = Optional.ofNullable((JSONArray) claimsSet.getClaims().get("roles"))
            .filter(r -> !r.isEmpty())
            .orElse(DEFAULT_ROLE_CLAIM);

        // By default the authenticated is set to true as part PreAuthenticatedAuthenticationToken constructor.
        SecurityContextHolder
            .getContext()
            .setAuthentication(
                new PreAuthenticatedAuthenticationToken(
                    new UserPrincipal(null,null, claimsSet),
                    null,
                    rolesToGrantedAuthorities(roles)
                ));
      } else {
        SecurityContextHolder
            .getContext()
            .setAuthentication(
                new PreAuthenticatedAuthenticationToken(
                    null, null, null
                ));
      }
    } catch (ParseException ex) {
      LOGGER.error("Failed to initialize UserPrincipal.", ex);
      throw new AppException(500, "Unable to parse claims in istio payload", ex.getMessage());
    }
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * To return roles.
   * @param roles Request Object.
   * @return set representation of roles.
   */
  protected Set<SimpleGrantedAuthority> rolesToGrantedAuthorities(final JSONArray roles) {
    return roles.stream()
        .filter(Objects::nonNull)
        .map(s -> new SimpleGrantedAuthority(ROLE_PREFIX + s))
        .collect(Collectors.toSet());
  }
}
