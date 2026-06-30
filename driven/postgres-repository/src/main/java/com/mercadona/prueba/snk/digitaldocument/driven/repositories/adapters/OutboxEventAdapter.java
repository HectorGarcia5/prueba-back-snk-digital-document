package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import com.mercadona.framework.cna.lib.outbox.avro.jpa.register.service.OutBoxAvroJPAService;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutKey;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutValue;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventAdapter implements OutboxEventPort {

  private final OutBoxAvroJPAService outBoxAvroJPAService;

  @Value("${outbox.topic.employee-digital-document}")
  private String topic;

  @Override
  public void saveForPublication(UUID documentId, String employeeId, String managedGroupId) {
    var key = EmployeeDigitalDocumentEventRestrictedOutKey.newBuilder()
        .setEmployeeId(employeeId)
        .setManagedGroupId(managedGroupId)
        .build();

    var value = EmployeeDigitalDocumentEventRestrictedOutValue.newBuilder()
        .setDigitalDocumentId(documentId.toString())
        .setEmployeeId(employeeId)
        .setManagedGroupId(managedGroupId)
        .build();

    outBoxAvroJPAService.save(key, value, topic);
    log.info("event=OUTBOX_SAVED documentId={}", documentId);
  }
}
