package com.mercadona.prueba.snk.digitaldocument.application.services;

import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxEvent;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentRepository;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxRepository;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles all document state transitions as atomic DB operations.
 * Each method runs in a single transaction so the domain state and
 * any side-effects (e.g. Outbox event creation) are committed together.
 */
@Service
@RequiredArgsConstructor
public class DocumentStateService {

  private final DocumentRepository documentRepository;
  private final OutboxRepository outboxRepository;

  @Transactional
  public void transitionToEnriched(DigitalDocument document, EmployeeData employeeData) {
    document.markEnriched(employeeData);
    documentRepository.save(document);
  }

  @Transactional
  public void transitionToPdfGenerated(DigitalDocument document, String checksum) {
    document.markPdfGenerated(checksum);
    documentRepository.save(document);
  }

  /**
   * Atomically transitions to STORED and creates the Outbox event.
   * This is the critical consistency boundary: no Kafka event is ever published
   * unless the document has been successfully persisted as STORED.
   */
  @Transactional
  public void transitionToStored(DigitalDocument document, String storageKey) {
    document.markStored(storageKey);
    documentRepository.save(document);
    outboxRepository.save(OutboxEvent.createInitial(
        document.getId(), document.getEmployeeId(), document.getManagedGroupId()));
  }

  @Transactional
  public void transitionToFailed(DigitalDocument document, FailedStep step,
      String errorCode, String rawMessage) {
    document.markFailed(step, errorCode, sanitize(rawMessage));
    documentRepository.save(document);
  }

  private String sanitize(String message) {
    if (message == null) return "Unknown error";
    return message.length() > 500 ? message.substring(0, 500) : message;
  }
}
