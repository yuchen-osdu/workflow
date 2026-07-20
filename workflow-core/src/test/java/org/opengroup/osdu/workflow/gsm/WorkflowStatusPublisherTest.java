package org.opengroup.osdu.workflow.gsm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.status.Status;
import org.opengroup.osdu.core.common.status.IEventPublisher;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link WorkflowStatusPublisher}
 */
@ExtendWith(MockitoExtension.class)
class WorkflowStatusPublisherTest {

  private static final String TEST_CORRELATION_ID = "TEST_CORRELATION_ID";
  private static final String TEST_USER_EMAIL = "TEST_USER_EMAIL";
  private static final String TEST_RUN_ID = "TEST_RUN_ID";

  @Mock
  private IEventPublisher statusEventPublisher;

  @Mock
  private DpsHeaders dpsHeaders;

  private WorkflowStatusPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new WorkflowStatusPublisher(statusEventPublisher);
    Mockito.when(dpsHeaders.getCorrelationId()).thenReturn(TEST_CORRELATION_ID);
    Mockito.when(dpsHeaders.getUserEmail()).thenReturn(TEST_USER_EMAIL);
  }

  @Test
  void shouldPublishStatusWithNoErrors() {
    //given
    Mockito.doNothing().when(statusEventPublisher).publish(any(), any());

    //when
    publisher.publishStatusWithNoErrors(TEST_RUN_ID, dpsHeaders, WorkflowStatusPublisher.WORKFLOW_SUCCESS, Status.SUCCESS);

    //then
    Mockito.verify(statusEventPublisher, Mockito.times(1))
        .publish(any(), any());
  }

  @Test
  void shouldPublishStatusWithUnexpectedErrors() {
    //given
    Mockito.doNothing().when(statusEventPublisher).publish(any(), any());

    //when
    publisher.publishStatusWithUnexpectedErrors(TEST_RUN_ID, dpsHeaders, WorkflowStatusPublisher.WORKFLOW_FINISHED, Status.FAILED);

    //then
    Mockito.verify(statusEventPublisher, Mockito.times(1))
        .publish(any(), any());
  }

  @Test
  void shouldSuppressUnexpectedException() {
    //given
    Mockito.doThrow(CoreException.class).when(statusEventPublisher).publish(any(), any());

    //when
    Assertions.assertDoesNotThrow(
        () -> publisher.publishStatusWithNoErrors(TEST_RUN_ID, dpsHeaders, WorkflowStatusPublisher.WORKFLOW_SUCCESS, Status.SUCCESS)
    );

    //then
    Mockito.verify(statusEventPublisher, Mockito.times(1))
        .publish(any(), any());
  }
}
