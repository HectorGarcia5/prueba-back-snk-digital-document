package com.mercadona.prueba.snk.digitaldocument.driven.repositories;

import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  @Query("""
      SELECT d FROM DigitalDocumentMO d
       WHERE d.status = :status
         AND d.retryCount < :maxRetries
         AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
       ORDER BY d.nextRetryAt NULLS FIRST, d.createdAt ASC
      """)
  List<DigitalDocumentMO> findFailedRetryable(
      Pageable pageable,
      @Param("status") DocumentStatus status,
      @Param("maxRetries") int maxRetries,
      @Param("now") OffsetDateTime now);
}
