package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.TopicConsumerErrorServicePort;
import com.mercadona.prueba.snk.digitaldocument.driving.kafka.serializer.AvroSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlExceptionService {

  private final TopicConsumerErrorServicePort errorService;

  private final AvroSerializer<SpecificRecordBase> serializer = new AvroSerializer<>();

  @SuppressWarnings("rawtypes")
  public void controlException(SpecificRecordBase key, SpecificRecordBase value,
                               Exception e, ConsumerRecord consumerRecord) {
    var rootCause = getRootCause(e);
    var model = TopicConsumerErrorModel.builder()
        .eventKey(serialize(key))
        .eventPayload(serialize(value))
        .eventOffset(Math.toIntExact(consumerRecord.offset()))
        .date(LocalDateTime.now())
        .error(rootCause.getMessage())
        .tceTopicName(consumerRecord.topic())
        .build();
    log.warn("event=TOPIC_CONSUMER_ERROR topic={} offset={} error={}",
        consumerRecord.topic(), consumerRecord.offset(), rootCause);
    errorService.saveUpdateTopicConsumerError(model);
  }

  private String serialize(SpecificRecordBase record) {
    if (record == null) return null;
    try {
      return serializer.serialize(record);
    } catch (Exception ex) {
      log.warn("Could not serialize Avro record for error log: {}", ex.getMessage());
      return record.toString();
    }
  }

  private Throwable getRootCause(Throwable t) {
    var cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }
}
