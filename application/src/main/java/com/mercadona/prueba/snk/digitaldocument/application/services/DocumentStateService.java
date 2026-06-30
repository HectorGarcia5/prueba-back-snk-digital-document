package com.mercadona.prueba.snk.digitaldocument.application.services;

import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentRepository;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxEventPort;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles document state transitions as atomic DB operations.
 * Each method runs in a single @Transactional so domain state and side-effects
 * (Outbox event creation) are committed together.
 */
@Service
@RequiredArgsConstructor
public class DocumentStateService {

  private final DocumentRepository documentRepository;
  private final OutboxEventPort outboxEventPort;

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
   * Atomically: STORED → Outbox event created → PUBLISHED.
   * All three operations commit together. If the transaction rolls back,
   * no Outbox event is created and the document stays in PDF_GENERATED.
   * The framework's auto-publisher handles Kafka delivery (at-least-once).
   */
  @Transactional
  public void transitionToStored(DigitalDocument document, String storageKey) {
    document.markStored(storageKey);
    documentRepository.save(document);
    outboxEventPort.saveForPublication(
        document.getId(), document.getEmployeeId(), document.getManagedGroupId());
    document.markPublished();
    documentRepository.save(document);
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
