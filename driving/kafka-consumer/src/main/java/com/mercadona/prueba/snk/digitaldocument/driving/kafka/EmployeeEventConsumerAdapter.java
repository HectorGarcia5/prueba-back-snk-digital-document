package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import com.mercadona.framework.cna.lib.kafka.consumers.KafkaConsumerListener;
import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxEvent;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxRepository;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ProcessDigitalDocumentPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ReceiveEmployeeEventPort;
import com.mercadona.prueba.snk.digitaldocument.application.usecases.ReceiveEmployeeEventResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import thirdparty.employee.employee.EmployeeEventPublicKey;
import thirdparty.employee.employee.EmployeeEventPublicValue;

@Slf4j
@Component
@Profile("!local")
public class EmployeeEventConsumerAdapter
    extends KafkaConsumerListener<EmployeeEventPublicKey, EmployeeEventPublicValue> {

  private final ReceiveEmployeeEventPort receiveUseCase;
  private final ProcessDigitalDocumentPort processUseCase;
  private final OutboxRepository outboxRepository;

  public EmployeeEventConsumerAdapter(
      @Value("${fwkcna.kafka.consumer.topics.groups.employee.main}") String[] topics,
      @Value("${fwkcna.kafka.consumer.topics.groups.employee.groupId}") String groupId,
      ReceiveEmployeeEventPort receiveUseCase,
      ProcessDigitalDocumentPort processUseCase,
      OutboxRepository outboxRepository) {
    super(topics, groupId);
    this.receiveUseCase = receiveUseCase;
    this.processUseCase = processUseCase;
    this.outboxRepository = outboxRepository;
  }

  @Override
  public void consume(ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record) {
    if (record == null || record.key() == null) {
      log.error("event=CONSUMER_NULL_KEY — skipping");
      return;
    }

    // Extract from KEY: key.getId() = employeeId, key.getManagedGroupId().getId() = managedGroupId
    var key = record.key();
    String employeeId = String.valueOf(key.getId());
    String managedGroupId = String.valueOf(key.getManagedGroupId().getId());

    log.info("event=CONSUMER_RECEIVED employeeId={} managedGroupId={}", employeeId, managedGroupId);

    try {
      var response = receiveUseCase.receive(employeeId, managedGroupId);
      var result = response.result();
      var documentId = response.documentId();

      switch (result) {
        case CREATED -> {
          log.info("event=CONSUMER_CREATED documentId={}", documentId);
          processUseCase.process(documentId);
        }
        case DUPLICATE_READY_TO_REPUBLISH -> {
          log.info("event=CONSUMER_DUPLICATE_REPUBLISH documentId={}", documentId);
          // Create a duplicate Outbox event so the publisher re-sends to employee-digital-document
          outboxRepository.save(OutboxEvent.createForDuplicate(documentId, employeeId, managedGroupId));
        }
        case DUPLICATE_IN_PROGRESS ->
          log.info("event=CONSUMER_DUPLICATE_IN_PROGRESS documentId={}", documentId);
        case DUPLICATE_FAILED ->
          log.info("event=CONSUMER_DUPLICATE_FAILED documentId={}", documentId);
      }
    } catch (Exception e) {
      log.error("event=CONSUMER_ERROR employeeId={} managedGroupId={} error={}",
          employeeId, managedGroupId, e.getMessage(), e);
      throw e;
    }
  }
}
