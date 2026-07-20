/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sun.jersey.api.client.ClientResponse;
import javax.ws.rs.HttpMethod;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.workflow.consts.TestConstants;
import org.opengroup.osdu.workflow.util.TestBase;
import org.opengroup.osdu.workflow.util.VersionInfoUtils;

public abstract class GetServiceInfoIntegrationTest extends TestBase {

  protected static final VersionInfoUtils VERSION_INFO_UTILS = new VersionInfoUtils();

  @Test
  public void should_returnInfo() {
    ClientResponse response = callInfoApi(TestConstants.GET_SERVICE_INFO_URL);

    VersionInfoUtils.VersionInfo responseObject = VERSION_INFO_UTILS.getVersionInfoFromResponse(response);

    validateInfoObjectProperties(responseObject);
  }

  @Test
  public void should_returnInfo_withTrailingSlash() {
    ClientResponse response = callInfoApi(TestConstants.GET_SERVICE_INFO_URL+"/");

    VersionInfoUtils.VersionInfo responseObject = VERSION_INFO_UTILS.getVersionInfoFromResponse(response);

    validateInfoObjectProperties(responseObject);
  }

  private void validateInfoObjectProperties(VersionInfoUtils.VersionInfo versionInfo) {
    assertNotNull(versionInfo.groupId);
    assertNotEquals("", versionInfo.groupId);

    assertNotNull(versionInfo.artifactId);
    assertNotEquals("", versionInfo.artifactId);

    assertNotNull(versionInfo.version);
    assertNotEquals("", versionInfo.version);

    assertNotNull(versionInfo.buildTime);
    assertNotEquals("", versionInfo.buildTime);

    assertNotNull(versionInfo.branch);
    assertNotEquals("", versionInfo.branch);

    assertNotNull(versionInfo.commitId);
    assertNotEquals("", versionInfo.commitId);

    assertNotNull(versionInfo.commitMessage);
    assertNotEquals("", versionInfo.commitMessage);
  }

  private ClientResponse callInfoApi(String url) {
    ClientResponse response = client.send(
        HttpMethod.GET,
        url,
        null,
        headers,
        ""
    );

    assertEquals(HttpStatus.SC_OK, response.getStatus(), response.toString());
    return response;
  }
}
