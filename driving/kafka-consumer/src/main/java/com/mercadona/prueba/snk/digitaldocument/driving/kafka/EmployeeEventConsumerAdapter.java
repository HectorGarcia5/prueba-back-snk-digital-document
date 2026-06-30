package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import com.mercadona.framework.cna.lib.kafka.consumer.MercadonaKafkaManualAckConsumerListener;
import com.mercadona.framework.cna.lib.outbox.avro.jpa.register.service.OutBoxAvroJPAService;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ProcessDigitalDocumentPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ReceiveEmployeeEventPort;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import thirdparty.employee.employee.EmployeeEventPublicKey;
import thirdparty.employee.employee.EmployeeEventPublicValue;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutKey;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutValue;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@Profile("!local")
public class EmployeeEventConsumerAdapter
    extends MercadonaKafkaManualAckConsumerListener<EmployeeEventPublicKey, EmployeeEventPublicValue> {

  private final ReceiveEmployeeEventPort receiveUseCase;
  private final ProcessDigitalDocumentPort processUseCase;
  private final OutBoxAvroJPAService outBoxAvroJPAService;

  @Value("${outbox.topic.employee-digital-document}")
  private String outputTopic;

  public EmployeeEventConsumerAdapter(
      @Value("${fwkcna.kafka.consumer.topics.input-0}") final String[] topics,
      @Value("${fwkcna.kafka.consumer.topics.groupid-0}") final String groupId,
      ReceiveEmployeeEventPort receiveUseCase,
      ProcessDigitalDocumentPort processUseCase,
      OutBoxAvroJPAService outBoxAvroJPAService) {
    super(topics, groupId);
    this.receiveUseCase = receiveUseCase;
    this.processUseCase = processUseCase;
    this.outBoxAvroJPAService = outBoxAvroJPAService;
  }

  @Override
  public void consume(
      @Nullable ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> consumerRecord,
      Acknowledgment ack) {

    if (consumerRecord == null || consumerRecord.key() == null) {
      log.error("event=CONSUMER_NULL_KEY");
      ack.acknowledge();
      return;
    }

    var key = consumerRecord.key();
    String employeeId    = String.valueOf(key.getId());
    String managedGroupId = String.valueOf(key.getManagedGroupId().getId());

    log.info("event=CONSUMER_RECEIVED employeeId={} managedGroupId={}", employeeId, managedGroupId);

    try {
      var response  = receiveUseCase.receive(employeeId, managedGroupId);
      var result    = response.result();
      var documentId = response.documentId();

      switch (result) {
        case CREATED -> {
          log.info("event=CONSUMER_CREATED documentId={}", documentId);
          processUseCase.process(documentId);
        }
        case DUPLICATE_READY_TO_REPUBLISH -> {
          log.info("event=CONSUMER_DUPLICATE_REPUBLISH documentId={}", documentId);
          publishToOutbox(documentId, employeeId, managedGroupId);
        }
        case DUPLICATE_IN_PROGRESS ->
            log.info("event=CONSUMER_DUPLICATE_IN_PROGRESS documentId={}", documentId);
        case DUPLICATE_FAILED ->
            log.info("event=CONSUMER_DUPLICATE_FAILED documentId={}", documentId);
      }

      ack.acknowledge();

    } catch (Exception e) {
      log.error("event=CONSUMER_ERROR employeeId={} managedGroupId={} error={}",
          employeeId, managedGroupId, e.getMessage(), e);
      ack.nack(Duration.ZERO);
    }
  }

  private void publishToOutbox(UUID documentId, String employeeId, String managedGroupId) {
    var avroKey = buildKey(documentId, employeeId, managedGroupId);
    var avroValue = buildValue(documentId, employeeId, managedGroupId);
    outBoxAvroJPAService.save(avroKey, avroValue, outputTopic);
  }

  private EmployeeDigitalDocumentEventRestrictedOutKey buildKey(
      UUID documentId, String employeeId, String managedGroupId) {
    return EmployeeDigitalDocumentEventRestrictedOutKey.newBuilder()
        .setEmployeeId(employeeId)
        .setManagedGroupId(managedGroupId)
        .build();
  }

  private EmployeeDigitalDocumentEventRestrictedOutValue buildValue(
      UUID documentId, String employeeId, String managedGroupId) {
    return EmployeeDigitalDocumentEventRestrictedOutValue.newBuilder()
        .setDigitalDocumentId(documentId.toString())
        .setEmployeeId(employeeId)
        .setManagedGroupId(managedGroupId)
        .build();
  }
}
