package com.mercadona.prueba.snk.digitaldocument.application.outbox;

public enum PublicationReason {
  /** First publication triggered by a new employee event. */
  INITIAL,
  /** Duplicate event received — document already existed. */
  DUPLICATE_EVENT,
  /** Explicit retry requested (by batch or admin endpoint). */
  MANUAL_RETRY
}
