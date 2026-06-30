package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

  OutboxEvent save(OutboxEvent event);

  /** Returns up to {@code limit} PENDING events ready to publish, using FOR UPDATE SKIP LOCKED. */
  List<OutboxEvent> findPendingForPublishing(int limit);

  void markPublished(UUID id, OffsetDateTime publishedAt);

  void markFailed(UUID id, int attempts, OffsetDateTime nextAttemptAt);
}
