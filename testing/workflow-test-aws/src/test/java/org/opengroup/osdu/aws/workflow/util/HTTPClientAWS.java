/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.aws.workflow.util;

import org.opengroup.osdu.workflow.util.HTTPClient;
import java.util.concurrent.atomic.AtomicReference;

public class HTTPClientAWS extends HTTPClient {
  private static final String TOKEN_PREFIX = "Bearer ";

  private final AtomicReference<String> cachedAccessToken = new AtomicReference<>();
  private final AtomicReference<String> cachedNoAccessToken = new AtomicReference<>();

  @Override
  public synchronized String getAccessToken() throws Exception {
    String currentToken = cachedAccessToken.get();
    if (currentToken == null) {
      currentToken = TOKEN_PREFIX + JwtTokenUtilV2.getAccessToken();
      cachedAccessToken.set(currentToken);
    }
    return currentToken;
  }

  @Override
  public synchronized String getNoDataAccessToken() throws Exception {
    String currentToken = cachedNoAccessToken.get();
    if (currentToken == null) {
      currentToken = TOKEN_PREFIX + JwtTokenUtilV2.getNoAccessToken();
      cachedNoAccessToken.set(currentToken);
    }
    return currentToken;
  }
}
