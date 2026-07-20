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

import org.opengroup.osdu.core.aws.v2.cognito.AWSCognitoClient;

public class JwtTokenUtilV2 {
  private static final String COGNITO_AUTH_PARAMS_USER_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_USER";
  private static final String COGNITO_AUTH_PARAMS_USER_NO_ACCESS_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS";
  private static final String COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_PASSWORD";

  private static final AWSCognitoClient cognitoClient = new AWSCognitoClient();

  private JwtTokenUtilV2() {
    // Prevent instantiation
  }

  public static String getAccessToken() {
    String username = System.getProperty(COGNITO_AUTH_PARAMS_USER_PROPERTY, System.getenv(COGNITO_AUTH_PARAMS_USER_PROPERTY));
    String password = System.getProperty(COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY, System.getenv(COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY));
    return cognitoClient.getToken(username, password);
  }

  public static String getNoAccessToken() {
    String username = System.getProperty(COGNITO_AUTH_PARAMS_USER_NO_ACCESS_PROPERTY, System.getenv(COGNITO_AUTH_PARAMS_USER_NO_ACCESS_PROPERTY));
    String password = System.getProperty(COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY, System.getenv(COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY));
    return cognitoClient.getToken(username, password);
  }
}