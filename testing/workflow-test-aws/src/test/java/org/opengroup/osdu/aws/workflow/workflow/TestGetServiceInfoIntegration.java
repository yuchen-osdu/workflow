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

package org.opengroup.osdu.aws.workflow.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opengroup.osdu.aws.workflow.util.HTTPClientAWS;
import org.opengroup.osdu.workflow.workflow.GetServiceInfoIntegrationTest;

public class TestGetServiceInfoIntegration extends GetServiceInfoIntegrationTest {

  @BeforeEach
  @Override
  public void setup() throws Exception {
    this.client = new HTTPClientAWS();
    this.headers = client.getCommonHeader();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    this.client = null;
    this.headers = null;
  }
}
