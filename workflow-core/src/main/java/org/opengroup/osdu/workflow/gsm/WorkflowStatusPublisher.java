package org.opengroup.osdu.workflow.gsm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.status.Message;
import org.opengroup.osdu.core.common.model.status.Status;
import org.opengroup.osdu.core.common.model.status.StatusDetails;
import org.opengroup.osdu.core.common.status.AttributesBuilder;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStatusPublisher {

  public static final String WORKFLOW_SUBMITTED = "Workflow run submitted. ";
  public static final String WORKFLOW_IN_PROGRESS = "Workflow execution started. ";
  public static final String WORKFLOW_SUCCESS = "Workflow execution successfully finished. ";
  public static final String WORKFLOW_FAILED = "Workflow execution failed. ";
  public static final String WORKFLOW_FINISHED = "Workflow execution finished. ";

  public static final String USER_MADE_CHANGE = "Changed manually by user. ";

  private static final String FAILED_TO_PUBLISH_STATUS = "Failed to publish status. ";
  private static final String KIND = "status";
  private static final String STAGE_WORKFLOW = "WORKFLOW";
  private static final String DEFAULT_VERSION = "1";

  private static final int NO_ERRORS = 0;
  private static final int UNEXPECTED_FAILURE = 500;

  private static final String LOG_TEMPLATE = "Going to publish GMS update for workflow with id='%s' and status='%s'. ";

  private final IEventPublisher statusEventPublisher;

  public void publishStatusWithNoErrors(String runId, DpsHeaders dpsHeaders, String msg, Status status) {
    StatusDetails statusDetails = createStatusDetails(msg, runId, status, NO_ERRORS, dpsHeaders);
    log.debug(String.format(LOG_TEMPLATE, runId, status));

    publish(statusDetails, dpsHeaders);
  }

  public void publishStatusWithUnexpectedErrors(String runId, DpsHeaders dpsHeaders, String msg, Status status) {
    StatusDetails statusDetails = createStatusDetails(msg, runId, status, UNEXPECTED_FAILURE, dpsHeaders);
    log.debug(String.format(LOG_TEMPLATE, runId, status));

    publish(statusDetails, dpsHeaders);
  }

  private StatusDetails createStatusDetails(String msg, String recordId,
                                            Status status,
                                            int errorCode, DpsHeaders dpsHeaders) {
    StatusDetails.Properties properties = createProperties(msg, recordId, status, errorCode, dpsHeaders);
    StatusDetails statusDetails = new StatusDetails();
    statusDetails.setKind(KIND);
    statusDetails.setProperties(properties);

    return statusDetails;
  }

  private StatusDetails.Properties createProperties(String msg, String recordId,
                                                    Status status, int errorCode, DpsHeaders dpsHeaders) {
    StatusDetails statusDetails = new StatusDetails();
    StatusDetails.Properties properties = statusDetails.new Properties();
    properties.setCorrelationId(dpsHeaders.getCorrelationId());
    properties.setErrorCode(errorCode);
    properties.setMessage(msg);
    properties.setRecordId(recordId);
    properties.setRecordIdVersion(WorkflowStatusPublisher.DEFAULT_VERSION);
    properties.setStage(STAGE_WORKFLOW);
    properties.setStatus(status);
    properties.setTimestamp(System.currentTimeMillis());
    properties.setUserEmail(dpsHeaders.getUserEmail());

    return properties;
  }

  private void publish(StatusDetails statusDetails, DpsHeaders dpsHeaders) {
    try {
      Message[] messages = new StatusDetails[]{statusDetails};
      AttributesBuilder attributesBuilder = new AttributesBuilder(dpsHeaders);
      statusEventPublisher.publish(messages, attributesBuilder.createAttributesMap());
    } catch (CoreException e) {
      log.warn(FAILED_TO_PUBLISH_STATUS + e.getMessage());
    }
  }
}
