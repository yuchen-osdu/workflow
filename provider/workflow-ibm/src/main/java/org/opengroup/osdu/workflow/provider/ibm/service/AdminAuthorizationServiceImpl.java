package org.opengroup.osdu.workflow.provider.ibm.service;

import org.opengroup.osdu.workflow.provider.interfaces.IAdminAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AdminAuthorizationServiceImpl implements IAdminAuthorizationService {
  @Override
  public boolean isDomainAdminServiceAccount() {
	  final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	  Jwt principal = (Jwt) authentication.getPrincipal();
	  Boolean isRootUser = principal.getClaimAsBoolean("rootUser");
	  if(isRootUser != null && isRootUser) {
		  log.debug("logged in user is root user");
		  return true;
	  }
	  log.error("logged in user is not root user.");
    return false;
  }
}
