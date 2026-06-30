package com.mercadona.prueba.snk.digitaldocument.application.usecases;

import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentRepository;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ReceiveEmployeeEventPort;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.DocumentAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveEmployeeEventUseCase implements ReceiveEmployeeEventPort {

  private final DocumentRepository documentRepository;

  /**
   * Receives an employee event and guarantees idempotence:
   * <ol>
   *   <li>Fast path: check if document already exists (covers non-concurrent duplicates).</li>
   *   <li>Slow path: try to create. The PostgreSQL UNIQUE constraint is the final guarantee.</li>
   *   <li>Race condition: if concurrent save fails, fetch the winner and classify it.</li>
   * </ol>
   * Not annotated with @Transactional intentionally: each repository call runs its own
   * transaction so the DataIntegrityViolation translated by the adapter doesn't poison
   * an outer transaction context.
   */
  @Override
  public ReceiveEmployeeEventResult receive(String employeeId, String managedGroupId) {
    log.info("event=RECEIVE employeeId={} managedGroupId={}", employeeId, managedGroupId);

    var existing = documentRepository.findByEmployeeIdAndManagedGroupId(employeeId, managedGroupId);
    if (existing.isPresent()) {
      var result = classify(existing.get().getStatus());
      log.info("event=DUPLICATE employeeId={} managedGroupId={} result={}", employeeId, managedGroupId, result);
      return result;
    }

    try {
      var saved = documentRepository.save(DigitalDocument.createPending(employeeId, managedGroupId));
      log.info("event=CREATED documentId={} employeeId={} managedGroupId={}", saved.getId(), employeeId, managedGroupId);
      return ReceiveEmployeeEventResult.CREATED;
    } catch (DocumentAlreadyExistsException e) {
      log.warn("event=CONCURRENT_DUPLICATE employeeId={} managedGroupId={}", employeeId, managedGroupId);
      return documentRepository
          .findByEmployeeIdAndManagedGroupId(employeeId, managedGroupId)
          .map(doc -> classify(doc.getStatus()))
          .orElseThrow(() -> new IllegalStateException(
              "Document must exist after unique constraint violation [employeeId=" + employeeId
              + ", managedGroupId=" + managedGroupId + "]"));
    }
  }

  private ReceiveEmployeeEventResult classify(DocumentStatus status) {
    return switch (status) {
      case STORED, PUBLISHED -> ReceiveEmployeeEventResult.DUPLICATE_READY_TO_REPUBLISH;
      case FAILED            -> ReceiveEmployeeEventResult.DUPLICATE_FAILED;
      default                -> ReceiveEmployeeEventResult.DUPLICATE_IN_PROGRESS;
    };
  }
}
