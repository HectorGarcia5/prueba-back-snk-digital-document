package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

  DigitalDocument save(DigitalDocument document);

  Optional<DigitalDocument> findById(UUID id);

  Optional<DigitalDocument> findByEmployeeIdAndManagedGroupId(String employeeId, String managedGroupId);

  List<DigitalDocument> findByStatus(DocumentStatus status, int page, int size);

  long countByStatus(DocumentStatus status);

  /** Acquires a pessimistic write lock on the document row. */
  Optional<DigitalDocument> lockById(UUID id);

  /**
   * Returns up to {@code limit} FAILED documents eligible for retry,
   * using FOR UPDATE SKIP LOCKED to allow safe concurrent batch processing.
   */
  List<DigitalDocument> findFailedRetryable(int limit, int maxRetries);
}
