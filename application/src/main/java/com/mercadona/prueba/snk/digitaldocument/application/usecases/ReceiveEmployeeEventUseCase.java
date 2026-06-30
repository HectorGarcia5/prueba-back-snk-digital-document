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
   * Not @Transactional: each repository call has its own transaction so the
   * DataIntegrityViolation translated by the adapter doesn't poison an outer tx.
   */
  @Override
  public ReceiveEmployeeEventResponse receive(String employeeId, String managedGroupId) {
    log.info("event=RECEIVE employeeId={} managedGroupId={}", employeeId, managedGroupId);

    var existing = documentRepository.findByEmployeeIdAndManagedGroupId(employeeId, managedGroupId);
    if (existing.isPresent()) {
      var doc = existing.get();
      var result = classify(doc.getStatus());
      log.info("event=DUPLICATE employeeId={} managedGroupId={} result={}", employeeId, managedGroupId, result);
      return new ReceiveEmployeeEventResponse(result, doc.getId());
    }

    try {
      var saved = documentRepository.save(DigitalDocument.createPending(employeeId, managedGroupId));
      log.info("event=CREATED documentId={} employeeId={} managedGroupId={}", saved.getId(), employeeId, managedGroupId);
      return new ReceiveEmployeeEventResponse(ReceiveEmployeeEventResult.CREATED, saved.getId());
    } catch (DocumentAlreadyExistsException e) {
      log.warn("event=CONCURRENT_DUPLICATE employeeId={} managedGroupId={}", employeeId, managedGroupId);
      var doc = documentRepository
          .findByEmployeeIdAndManagedGroupId(employeeId, managedGroupId)
          .orElseThrow(() -> new IllegalStateException(
              "Document must exist after unique constraint violation [employeeId=" + employeeId
              + ", managedGroupId=" + managedGroupId + "]"));
      return new ReceiveEmployeeEventResponse(classify(doc.getStatus()), doc.getId());
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
