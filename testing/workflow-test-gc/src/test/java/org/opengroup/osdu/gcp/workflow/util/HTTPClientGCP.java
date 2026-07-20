/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package org.opengroup.osdu.gcp.workflow.util;

import lombok.extern.java.Log;
import org.opengroup.osdu.workflow.util.HTTPClient;

import java.util.HashMap;
import java.util.Map;

import static org.opengroup.osdu.workflow.consts.DefaultVariable.*;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_DATA_PARTITION_ID;

@Log
public class HTTPClientGCP extends HTTPClient {
  @Override
  public synchronized String getAccessToken() throws Exception {
    if (accessToken == null || accessToken.isEmpty()) {
      log.info("Get INTEGRATION_TESTER credentials");
      accessToken =
          new GoogleServiceAccount(getEnvironmentVariableOrDefaultKey(INTEGRATION_TESTER))
              .getAuthToken();
    }
    return "Bearer " + accessToken;
  }

  @Override
  public synchronized String getNoDataAccessToken() throws Exception {
    if (noDataAccessToken == null || noDataAccessToken.isEmpty()) {
      log.info("Get NO_DATA_ACCESS_TESTER credentials");
      noDataAccessToken =
          new GoogleServiceAccount(getEnvironmentVariableOrDefaultKey(NO_DATA_ACCESS_TESTER))
              .getAuthToken();
    }
    return "Bearer " + noDataAccessToken;
  }

  public Map<String, String> getCommonHeaderWithoutPartition() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HEADER_DATA_PARTITION_ID, "");
    return headers;
  }
}
