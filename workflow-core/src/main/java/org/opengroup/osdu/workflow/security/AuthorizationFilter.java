/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.exception.BadRequestException;
import org.opengroup.osdu.core.common.exception.UnauthorizedException;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.workflow.provider.interfaces.IAdminAuthorizationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Slf4j
@RequiredArgsConstructor
@Component("authorizationFilter")
@RequestScope
public class AuthorizationFilter {

  final IAuthorizationService authorizationService;

  final DpsHeaders headers;

  final IAdminAuthorizationService adminAuthorizationService;

  /**
   * Check the access permission for provided Authorization header and the required roles.
   *
   * @param requiredRoles required roles
   * @return true if the user groups by the authorization token have any required roles otherwise
   * false
   */
  public boolean hasPermission(String... requiredRoles) {
    validateMandatoryHeaders();
    if (StringUtils.isEmpty(this.headers.getPartitionId())) {
      throw new BadRequestException("data-partition-id header is mandatory");
    }
    AuthorizationResponse authResponse = authorizationService.authorizeAny(headers, requiredRoles);
    headers.put(DpsHeaders.USER_EMAIL, authResponse.getUser());
    return true;
  }

  public boolean hasRootPermission() {
    validateMandatoryHeaders();
    if (!StringUtils.isEmpty(this.headers.getPartitionId())) {
      throw new BadRequestException("data-partition-id header should not be passed");
    }
    headers.put(DpsHeaders.USER_EMAIL, "RootUser");
    return adminAuthorizationService.isDomainAdminServiceAccount();
  }

  private void validateMandatoryHeaders() {
    if (StringUtils.isEmpty(this.headers.getAuthorization())) {
      throw new UnauthorizedException("Authorization header is mandatory");
    }
  }
}
