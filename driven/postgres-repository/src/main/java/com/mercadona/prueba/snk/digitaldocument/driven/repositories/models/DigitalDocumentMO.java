package com.mercadona.prueba.snk.digitaldocument.driven.repositories.models;

import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "digital_document",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_digital_document_employee",
        columnNames = {"employee_id", "managed_group_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalDocumentMO {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "employee_id", nullable = false, updatable = false)
  private String employeeId;

  @Column(name = "managed_group_id", nullable = false, updatable = false)
  private String managedGroupId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private DocumentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "failed_step")
  private FailedStep failedStep;

  // Full enriched employee data serialized as JSON (see V1.1.0 migration)
  @Column(name = "employee_data", columnDefinition = "TEXT")
  private String employeeData;

  @Column(name = "storage_key")
  private String storageKey;

  @Column(name = "checksum", length = 64)
  private String checksum;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_retry_at")
  private OffsetDateTime nextRetryAt;

  @Column(name = "last_error_code")
  private String lastErrorCode;

  @Column(name = "last_error_message", length = 1000)
  private String lastErrorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "published_at")
  private OffsetDateTime publishedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
