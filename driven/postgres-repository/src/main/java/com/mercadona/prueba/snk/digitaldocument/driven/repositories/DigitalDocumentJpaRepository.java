package com.mercadona.prueba.snk.digitaldocument.driven.repositories;

import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DigitalDocumentJpaRepository extends JpaRepository<DigitalDocumentMO, UUID> {

  Optional<DigitalDocumentMO> findByEmployeeIdAndManagedGroupId(String employeeId, String managedGroupId);

  List<DigitalDocumentMO> findByStatus(DocumentStatus status, Pageable pageable);

  long countByStatus(DocumentStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT d FROM DigitalDocumentMO d WHERE d.id = :id")
  Optional<DigitalDocumentMO> findByIdWithLock(@Param("id") UUID id);

  @Query(value = """
      SELECT * FROM digital_document
       WHERE status = 'FAILED'
         AND retry_count < :maxRetries
         AND (next_retry_at IS NULL OR next_retry_at <= now())
       ORDER BY next_retry_at NULLS FIRST, created_at ASC
       LIMIT :limit
       FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<DigitalDocumentMO> findFailedRetryable(@Param("limit") int limit, @Param("maxRetries") int maxRetries);
}
