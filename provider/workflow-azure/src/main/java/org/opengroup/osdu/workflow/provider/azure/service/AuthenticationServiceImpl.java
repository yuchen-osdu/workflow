//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.workflow.provider.azure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.exception.UnauthorizedException;
import org.opengroup.osdu.workflow.model.ClientResponse;
import org.opengroup.osdu.workflow.model.WorkflowEngineRequest;
import org.opengroup.osdu.workflow.provider.interfaces.IAuthenticationService;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements IAuthenticationService {

  private static Logger logger = Logger.getLogger(AuthenticationServiceImpl.class.getName());

  @Override
  public void checkAuthentication(String authorizationToken, String partitionID) {
    logger.log(Level.INFO, String.format("Start checking authentication. Authorization: {%s}, partitionID: {%s}",
      authorizationToken, partitionID));

    checkPreconditions(authorizationToken, partitionID);

    // TODO 22.06.21 (expires at 22.12.21): add check of user permissions

    logger.log(Level.INFO, "Finished checking authentication.");
  }

  private void checkPreconditions(String authorizationToken, String partitionID) {
    if (authorizationToken == null) {
      throw new UnauthorizedException("Missing authorization token");
    }

    if (partitionID == null) {
      throw new UnauthorizedException("Missing partitionID");
    }
  }
}
