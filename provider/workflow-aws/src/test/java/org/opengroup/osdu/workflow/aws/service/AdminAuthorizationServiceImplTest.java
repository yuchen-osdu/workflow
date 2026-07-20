package org.opengroup.osdu.workflow.aws.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceImplTest {

    @Mock
    private AdminAuthorizationServiceImpl adminAuthorizationService;

    /**
     * Test case to verify that isDomainAdminServiceAccount() always returns false.
     * This method checks the default behavior of the AdminAuthorizationServiceImpl
     * which is expected to return false for all cases.
     */
    @Test
    void test_isDomainAdminServiceAccount_returnsFalse() {

        Assertions.assertFalse(adminAuthorizationService.isDomainAdminServiceAccount());
    }

}
