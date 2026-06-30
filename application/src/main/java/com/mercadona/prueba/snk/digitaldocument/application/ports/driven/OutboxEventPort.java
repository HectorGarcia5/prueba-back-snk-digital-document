package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import java.util.UUID;

public interface OutboxEventPort {

  /** Saves an Avro outbox event for Kafka publication. Framework auto-publishes. */
  void saveForPublication(UUID documentId, String employeeId, String managedGroupId);
}
