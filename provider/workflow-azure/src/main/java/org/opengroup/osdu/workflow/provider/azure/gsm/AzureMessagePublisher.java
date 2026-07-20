package org.opengroup.osdu.workflow.provider.azure.gsm;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.azure.publisherFacade.MessagePublisher;
import org.opengroup.osdu.azure.publisherFacade.PublisherInfo;
import org.opengroup.osdu.core.common.exception.CoreException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.status.Message;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureMessagePublisher implements IEventPublisher {

  private static final String STATUS_CHANGED = "status-changed";
  private static final String EVENT_DATA_VERSION = "1.0";

  @Setter
  @Value("${azure.eventGrid.topicName}")
  private String eventGridTopic;

  @Setter
  @Value("${azure.serviceBus.topicName}")
  private String serviceBusTopic;

  private final MessagePublisher messagePublisher;

  @Override
  public void publish(Message[] messages, Map<String, String> attributesMap) throws CoreException {
    validateInput(messages, attributesMap);

    String dataPartitionId = attributesMap.get(DpsHeaders.DATA_PARTITION_ID);
    String correlationId = attributesMap.get(DpsHeaders.CORRELATION_ID);

    DpsHeaders dpsHeaders = new DpsHeaders();
    dpsHeaders.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
    dpsHeaders.put(DpsHeaders.CORRELATION_ID, correlationId);

    PublisherInfo publisherInfo = PublisherInfo.builder()
        .batch(messages)
        .eventGridTopicName(eventGridTopic)
        .eventGridEventSubject(STATUS_CHANGED)
        .eventGridEventType(STATUS_CHANGED)
        .eventGridEventDataVersion(EVENT_DATA_VERSION)
        .serviceBusTopicName(serviceBusTopic).build();

    messagePublisher.publishMessage(dpsHeaders, publisherInfo, Optional.empty());

    String logMsg = String.format(
        "Event published successfully to eventGridTopic='%s', ServiceBusTopic='%s' with dataPartitionId='%s' and correlationId='%s'.",
        eventGridTopic, serviceBusTopic, dataPartitionId, correlationId
    );
    log.info(logMsg);
  }

  private void validateInput(Message[] messages, Map<String, String> attributesMap) throws CoreException {
    validateMsg(messages);
    validateAttributesMap(attributesMap);
  }

  private void validateMsg(Message[] messages) throws CoreException {
    if (messages == null || messages.length == 0) {
      throw new CoreException("Nothing in message to publish");
    }
  }

  private void validateAttributesMap(Map<String, String> attributesMap) throws CoreException {
    if (attributesMap == null || attributesMap.isEmpty()) {
      throw new CoreException("data-partition-id and correlation-id are required to publish status event");
    } else if (attributesMap.get(DpsHeaders.DATA_PARTITION_ID) == null) {
      throw new CoreException("data-partition-id is required to publish status event");
    } else if (attributesMap.get(DpsHeaders.CORRELATION_ID) == null) {
      throw new CoreException("correlation-id is required to publish status event");
    }
  }
}
