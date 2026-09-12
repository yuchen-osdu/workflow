/*
 *  Copyright 2021-2026 Google LLC
 *  Copyright 2021-2026 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.workflow;

import java.util.List;
import org.opengroup.osdu.core.test.auth.UserType;
import org.opengroup.osdu.core.test.base.BaseGetInfoAcceptanceTests;
import org.opengroup.osdu.core.test.service.ServiceType;

/**
 * Validates the Workflow {@code /info} endpoint via the shared os-core-test base.
 *
 * <p>Inherits {@code should_returnInfo()} and {@code should_returnInfoWithTrailingSlash()} tests
 * from {@link BaseGetInfoAcceptanceTests}, which validate all standard version-info fields
 * (groupId, artifactId, version, buildTime, branch, commitId, commitMessage).
 */
public final class GetServiceInfoIntegrationTest extends BaseGetInfoAcceptanceTests {

    public GetServiceInfoIntegrationTest() {
        super(UserType.PRIVILEGED_USER, ServiceType.WORKFLOW_V1, List.of());
    }
}
