/*
 *  Copyright 2020-2025 Google LLC
 *  Copyright 2020-2025 EPAM Systems, Inc
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

package org.opengroup.osdu.workflow.validation;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.workflow.model.UpdateStatusRequest;
import org.opengroup.osdu.workflow.model.WorkflowStatusType;

@ExtendWith(MockitoExtension.class)
class CommonUpdateStatusRequestValidatorTest {

    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;
    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    @Test
    void givenStringWhenValidateThenExcludeSpecialCharacters() {
        // given

        // forbidden symbols
        HashSet<String> expressionLanguageSymbols = new HashSet<>(2);
        expressionLanguageSymbols.add("{$");
        expressionLanguageSymbols.add("}");

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setWorkflowId("not null");
        request.setWorkflowStatusType(WorkflowStatusType.QUEUED);
        CommonUpdateStatusRequestValidator validator = new CommonUpdateStatusRequestValidator();
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        // when
        when(context.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(any())).thenReturn(nodeBuilderCustomizableContext);

        // then
        // trigger the validation process
        validator.isValid(request, context);
        verify(context).buildConstraintViolationWithTemplate(argument.capture());
        String result = argument.getValue();
        boolean containsSymbol = expressionLanguageSymbols.stream()
            .anyMatch(result::contains);

        Assertions.assertFalse(containsSymbol, "String should not contain expression language symbols");
    }

}
