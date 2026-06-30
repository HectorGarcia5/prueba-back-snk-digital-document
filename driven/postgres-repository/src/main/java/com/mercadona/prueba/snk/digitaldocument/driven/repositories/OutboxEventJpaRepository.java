package com.mercadona.prueba.snk.digitaldocument.driven.repositories;

import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.OutboxEventMO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventMO, UUID> {

  @Query(value = """
      SELECT * FROM outbox_event
       WHERE status = 'PENDING'
         AND (next_attempt_at IS NULL OR next_attempt_at <= now())
       ORDER BY created_at ASC
       LIMIT :limit
       FOR UPDATE SKIP LOCKED
      """, nativeQuery = true)
  List<OutboxEventMO> findPendingForPublishing(@Param("limit") int limit);

  @Modifying
  @Query("UPDATE OutboxEventMO o SET o.status = 'PUBLISHED', o.publishedAt = :publishedAt WHERE o.id = :id")
  void markPublished(@Param("id") UUID id, @Param("publishedAt") OffsetDateTime publishedAt);

  @Modifying
  @Query("UPDATE OutboxEventMO o SET o.status = 'FAILED', o.attempts = :attempts, o.nextAttemptAt = :nextAttemptAt WHERE o.id = :id")
  void markFailed(@Param("id") UUID id, @Param("attempts") int attempts, @Param("nextAttemptAt") OffsetDateTime nextAttemptAt);
}
