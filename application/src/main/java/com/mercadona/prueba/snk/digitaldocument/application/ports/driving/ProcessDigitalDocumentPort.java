package com.mercadona.prueba.snk.digitaldocument.application.ports.driving;

import java.util.UUID;

public interface ProcessDigitalDocumentPort {

  /**
   * Advances a document one step forward based on its current status.
   * Idempotent: calling on a PUBLISHED document is a no-op.
   */
  void process(UUID documentId);
}
