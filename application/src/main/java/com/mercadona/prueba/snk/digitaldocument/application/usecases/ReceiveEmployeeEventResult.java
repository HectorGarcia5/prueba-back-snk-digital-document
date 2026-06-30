package com.mercadona.prueba.snk.digitaldocument.application.usecases;

public enum ReceiveEmployeeEventResult {

  /** New document created in PENDING state. */
  CREATED,

  /** Document exists and is still being processed (PENDING / ENRICHED / PDF_GENERATED). */
  DUPLICATE_IN_PROGRESS,

  /** Document exists but failed — eligible for batch retry. */
  DUPLICATE_FAILED,

  /** Document is STORED or PUBLISHED — republish the documentId to employee-digital-document. */
  DUPLICATE_READY_TO_REPUBLISH
}
