package com.mercadona.prueba.snk.digitaldocument.domain;

import com.mercadona.prueba.snk.digitaldocument.domain.exception.DigitalDocumentStateException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class DigitalDocument {

  private UUID id;
  private String employeeId;
  private String managedGroupId;
  private DocumentStatus status;
  private FailedStep failedStep;
  private EmployeeData employeeData;
  private String storageKey;
  private String checksum;
  private int retryCount;
  private OffsetDateTime nextRetryAt;
  private String lastErrorCode;
  private String lastErrorMessage;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private OffsetDateTime publishedAt;
  private Long version;

  // -------------------------------------------------------------------------
  // Factory
  // -------------------------------------------------------------------------

  public static DigitalDocument createPending(String employeeId, String managedGroupId) {
    OffsetDateTime now = OffsetDateTime.now();
    return DigitalDocument.builder()
        .id(UUID.randomUUID())
        .employeeId(employeeId)
        .managedGroupId(managedGroupId)
        .status(DocumentStatus.PENDING)
        .retryCount(0)
        .version(0L)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  // -------------------------------------------------------------------------
  // State transitions
  // -------------------------------------------------------------------------

  public void markEnriched(EmployeeData employeeData) {
    requireStatus(DocumentStatus.PENDING, "ENRICHED");
    this.employeeData = employeeData;
    this.status = DocumentStatus.ENRICHED;
    this.updatedAt = OffsetDateTime.now();
  }

  public void markPdfGenerated(String checksum) {
    requireStatus(DocumentStatus.ENRICHED, "PDF_GENERATED");
    this.checksum = checksum;
    this.status = DocumentStatus.PDF_GENERATED;
    this.updatedAt = OffsetDateTime.now();
  }

  public void markStored(String storageKey) {
    requireStatus(DocumentStatus.PDF_GENERATED, "STORED");
    this.storageKey = storageKey;
    this.status = DocumentStatus.STORED;
    this.updatedAt = OffsetDateTime.now();
  }

  public void markPublished() {
    requireStatus(DocumentStatus.STORED, "PUBLISHED");
    this.status = DocumentStatus.PUBLISHED;
    this.publishedAt = OffsetDateTime.now();
    this.updatedAt = this.publishedAt;
  }

  public void markFailed(FailedStep failedStep, String errorCode, String errorMessage) {
    if (this.status == DocumentStatus.PUBLISHED) {
      throw new DigitalDocumentStateException(
          "Cannot mark FAILED a document already PUBLISHED [id=" + id + "]");
    }
    this.status = DocumentStatus.FAILED;
    this.failedStep = failedStep;
    this.lastErrorCode = errorCode;
    this.lastErrorMessage = errorMessage;
    this.updatedAt = OffsetDateTime.now();
  }

  /**
   * Prepares the document for reprocessing from the step that failed.
   * Applies exponential backoff for nextRetryAt.
   */
  public void prepareForRetry() {
    if (this.status != DocumentStatus.FAILED) {
      throw new DigitalDocumentStateException(
          "Cannot prepare for retry a document not in FAILED state [id=" + id
          + ", status=" + status + "]");
    }
    long delaySeconds = calculateRetryDelaySeconds();
    this.status = this.failedStep.getRecoveryStatus();
    this.retryCount = this.retryCount + 1;
    this.nextRetryAt = OffsetDateTime.now().plusSeconds(delaySeconds);
    this.failedStep = null;
    this.lastErrorCode = null;
    this.lastErrorMessage = null;
    this.updatedAt = OffsetDateTime.now();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void requireStatus(DocumentStatus required, String targetName) {
    if (this.status != required) {
      throw new DigitalDocumentStateException(
          "Cannot transition to " + targetName + ": current status is " + this.status
          + " (expected " + required + ") [id=" + id + "]");
    }
  }

  /** Exponential backoff: 60s * 2^retryCount, capped at 1 hour. */
  private long calculateRetryDelaySeconds() {
    long base = 60L;
    long cap = 3_600L;
    return Math.min(base * (long) Math.pow(2, retryCount), cap);
  }
}
