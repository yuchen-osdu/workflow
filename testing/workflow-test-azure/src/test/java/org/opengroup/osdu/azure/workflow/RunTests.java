// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.azure.workflow;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@SelectPackages({"org.opengroup.osdu.azure.workflow.workflow", "org.opengroup.osdu.azure.workflow.v3", "org.opengroup.osdu.azure.workflow.operators"})
@ExcludeClassNamePatterns({"org.opengroup.osdu.azure.workflow.workflow.TestPostGetStatusIntegration",
    "org.opengroup.osdu.azure.workflow.workflow.TestPostStartWorkflowIntegration",
    "org.opengroup.osdu.azure.workflow.workflow.TestPostUpdateStatusIntegration"})
public class RunTests {

}
