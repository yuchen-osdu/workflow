/*
 *  Copyright 2021-2025 Google LLC
 *  Copyright 2021-2025 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.util;

import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import java.util.List;

public class VersionInfoUtils {

	public VersionInfo getVersionInfoFromResponse(ClientResponse response) {
		assertTrue(response.getType().toString().contains("application/json"));
		String json = response.getEntity(String.class);
		Gson gson = new Gson();
		return gson.fromJson(json, VersionInfo.class);
	}

	public class VersionInfo {
		public String groupId;
		public String artifactId;
		public String version;
		public String buildTime;
		public String branch;
		public String commitId;
		public String commitMessage;
    public List<ConnectedOuterService> connectedOuterServices;
	}

  public static class ConnectedOuterService {
    public String name;
    public String version;
  }
}
