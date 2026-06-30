package com.mercadona.prueba.snk.digitaldocument.domain;

public enum FailedStep {

  ENRICHMENT(DocumentStatus.PENDING),
  PDF_GENERATION(DocumentStatus.ENRICHED),
  STORAGE(DocumentStatus.PDF_GENERATED),
  PUBLICATION(DocumentStatus.STORED);

  private final DocumentStatus recoveryStatus;

  FailedStep(DocumentStatus recoveryStatus) {
    this.recoveryStatus = recoveryStatus;
  }

  public DocumentStatus getRecoveryStatus() {
    return recoveryStatus;
  }
}
