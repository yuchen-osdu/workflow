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

package org.opengroup.osdu.workflow.aws.security;

import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.Authentication;

@RunWith(MockitoJUnitRunner.class)
public class WhoamiControllerTest {

    private WhoamiController controller = new WhoamiController();

    @Mock
    SecurityContext securityContext;

    @Mock
    Authentication authentication;

    @Test
    public void whoamiTest() {
        final String username = "username";
        final String details = "some details";

        try (MockedStatic<SecurityContextHolder> mockedHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(username);
            when(authentication.getPrincipal()).thenReturn(details);
            when(authentication.getAuthorities()).thenReturn(new ArrayList<>());

            String pattern = String.format("user: %s<BR>roles: []<BR>details: %s<BR>", username, details);

            String result = controller.whoami();
            Assert.assertEquals(result, pattern);
        }
    }
}
