/*
 *  Copyright 2020-2026 Google LLC
 *  Copyright 2020-2026 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.workflow.provider.azure.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceImplTest {
  private static final String AUTHORIZATION_TOKEN = "authToken";
  private static final String PARTITION = "partition";

  @InjectMocks
  private AuthenticationServiceImpl authenticationService;

  @Test
  public void shouldCheckAuthentication() {
    authenticationService.checkAuthentication(AUTHORIZATION_TOKEN, PARTITION);
  }

  @Test
  public void should_throw_unauthorized_exception_when_token_isNull() {
    assertThrows(UnauthorizedException.class, () -> {
        authenticationService.checkAuthentication(null, PARTITION);
    });
  }

  @Test
  public void should_throw_unauthorized_exception_when_partition_isNull() {
    assertThrows(UnauthorizedException.class, () -> {
        authenticationService.checkAuthentication(AUTHORIZATION_TOKEN, null);
    });
  }

  @Test
  public void shouldThrowWhenNothingIsSpecified() {
    assertThrows(UnauthorizedException.class, () -> {
        authenticationService.checkAuthentication(null, null);
    });
  }
}
