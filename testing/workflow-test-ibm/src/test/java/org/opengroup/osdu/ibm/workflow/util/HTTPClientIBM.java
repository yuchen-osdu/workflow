/**
 * Copyright 2020 IBM Corp. All Rights Reserved.
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

package org.opengroup.osdu.ibm.workflow.util;


import static org.opengroup.osdu.workflow.consts.DefaultVariable.DEFAULT_DATA_PARTITION_ID_TENANT1;
import static org.opengroup.osdu.workflow.consts.DefaultVariable.getEnvironmentVariableOrDefaultKey;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_DATA_PARTITION_ID;
import static org.opengroup.osdu.workflow.consts.TestConstants.HEADER_USER;

import java.util.HashMap;
import java.util.Map;

import org.opengroup.osdu.core.ibm.util.IdentityClient;
import org.opengroup.osdu.workflow.util.HTTPClient;
import lombok.ToString;
import lombok.extern.java.Log;

@Log
@ToString
public class HTTPClientIBM extends HTTPClient {
	
	private static String token;
	private static String noDataAccesstoken;


	@Override
	public String getAccessToken() throws Exception {
        if(token == null) {
            try {
                token = "Bearer " + IdentityClient.getTokenForUserWithAccess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return token;
    }

	@Override
	public String getNoDataAccessToken() throws Exception {
        if(noDataAccesstoken == null) {
            try {
            	noDataAccesstoken = "Bearer " + IdentityClient.getTokenForUserWithNoAccess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return noDataAccesstoken;
    }
	
	@Override
	public Map<String, String> getCommonHeader() {
		Map<String, String> headers = new HashMap<>();
		headers.put(HEADER_DATA_PARTITION_ID, getEnvironmentVariableOrDefaultKey(DEFAULT_DATA_PARTITION_ID_TENANT1));
		//headers.put(HEADER_USER, "testUser");
		return headers;
	}

}
