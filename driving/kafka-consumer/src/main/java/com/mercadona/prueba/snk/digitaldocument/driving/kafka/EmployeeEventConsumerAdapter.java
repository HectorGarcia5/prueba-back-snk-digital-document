package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import com.mercadona.framework.cna.lib.kafka.consumer.MercadonaKafkaManualAckConsumerListener;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ProcessDigitalDocumentPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ReceiveEmployeeEventPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.RepublishDigitalDocumentPort;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import thirdparty.employee.employee.EmployeeEventPublicKey;
import thirdparty.employee.employee.EmployeeEventPublicValue;

@Slf4j
@Component
public class EmployeeEventConsumerAdapter
    extends MercadonaKafkaManualAckConsumerListener<EmployeeEventPublicKey, EmployeeEventPublicValue> {

  private final ReceiveEmployeeEventPort receiveUseCase;
  private final ProcessDigitalDocumentPort processUseCase;
  private final RepublishDigitalDocumentPort republishUseCase;
  private final ControlExceptionService controlExceptionService;

  public EmployeeEventConsumerAdapter(
      @Value("${kafka.employee.topic}") final String topic,
      @Value("${kafka.employee.group-id}") final String groupId,
      ReceiveEmployeeEventPort receiveUseCase,
      ProcessDigitalDocumentPort processUseCase,
      RepublishDigitalDocumentPort republishUseCase,
      ControlExceptionService controlExceptionService) {
    super(new String[]{topic}, groupId);
    this.receiveUseCase = receiveUseCase;
    this.processUseCase = processUseCase;
    this.republishUseCase = republishUseCase;
    this.controlExceptionService = controlExceptionService;
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
    String employeeId     = String.valueOf(key.getId());
    String managedGroupId = String.valueOf(key.getManagedGroupId().getId());

    log.info("event=CONSUMER_RECEIVED employeeId={} managedGroupId={}", employeeId, managedGroupId);

    try {
      var response   = receiveUseCase.receive(employeeId, managedGroupId);
      var result     = response.result();
      var documentId = response.documentId();

      switch (result) {
        case CREATED -> {
          log.info("event=CONSUMER_CREATED documentId={}", documentId);
          processUseCase.process(documentId);
        }
        case DUPLICATE_READY_TO_REPUBLISH -> {
          log.info("event=CONSUMER_DUPLICATE_REPUBLISH documentId={}", documentId);
          republishUseCase.republish(documentId, employeeId, managedGroupId);
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
      controlExceptionService.controlException(
          consumerRecord.key(), consumerRecord.value(), e, consumerRecord);
      ack.acknowledge();
    }
  }
}
