package com.mercadona.prueba.snk.digitaldocument.application.usecases;

import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxEventPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.RepublishDigitalDocumentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepublishDigitalDocumentUseCase implements RepublishDigitalDocumentPort {

  private final OutboxEventPort outboxEventPort;

  @Override
  public void republish(UUID documentId, String employeeId, String managedGroupId) {
    log.info("event=REPUBLISH documentId={} employeeId={} managedGroupId={}", documentId, employeeId, managedGroupId);
    outboxEventPort.saveForPublication(documentId, employeeId, managedGroupId);
  }
}
