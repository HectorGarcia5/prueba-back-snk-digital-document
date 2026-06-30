package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import com.mercadona.framework.cna.lib.outbox.avro.jpa.register.service.OutBoxAvroJPAService;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thirdparty.employee.employeedigitaldocument.v0.DataRecord;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocument;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutKey;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutValue;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentsRecord;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeIds;
import thirdparty.employee.employeedigitaldocument.v0.ManagedGroupIds;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class OutboxEventAdapter implements OutboxEventPort {

  private final OutBoxAvroJPAService outBoxAvroJPAService;

  public OutboxEventAdapter(@Qualifier("sr-basic") OutBoxAvroJPAService outBoxAvroJPAService) {
    this.outBoxAvroJPAService = outBoxAvroJPAService;
  }

  @Value("${outbox.topic.employee-digital-document}")
  private String topic;

  @Override
  public void saveForPublication(UUID documentId, String employeeId, String managedGroupId) {
    var key = EmployeeDigitalDocumentEventRestrictedOutKey.newBuilder()
        .setEmployeeId(employeeId)
        .setManagedGroupId(managedGroupId)
        .build();

    var value = EmployeeDigitalDocumentEventRestrictedOutValue.newBuilder()
        .setPayload(buildPayload(documentId, employeeId, managedGroupId))
        .build();

    outBoxAvroJPAService.save(key, value, topic);
    log.info("event=OUTBOX_SAVED documentId={}", documentId);
  }

  private EmployeeDigitalDocument buildPayload(UUID documentId, String employeeId, String managedGroupId) {
    var mgIds = ManagedGroupIds.newBuilder().setId(managedGroupId).build();
    var empIds = EmployeeIds.newBuilder().setId(employeeId).setManagedGroupId(mgIds).build();
    var docRecord = EmployeeDigitalDocumentsRecord.newBuilder().setId(documentId.toString()).build();
    var data = DataRecord.newBuilder()
        .setEmployeeData(empIds)
        .setEmployeeDigitalDocuments(List.of(docRecord))
        .build();
    return EmployeeDigitalDocument.newBuilder().setData(data).build();
  }
}
