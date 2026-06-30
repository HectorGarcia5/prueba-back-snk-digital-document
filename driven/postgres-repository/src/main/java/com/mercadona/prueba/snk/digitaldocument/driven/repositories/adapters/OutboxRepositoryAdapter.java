package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxEvent;
import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxStatus;
import com.mercadona.prueba.snk.digitaldocument.application.outbox.PublicationReason;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.OutboxEventJpaRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.OutboxEventMO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxRepositoryAdapter implements OutboxRepository {

  private final OutboxEventJpaRepository jpaRepository;

  @Override
  public OutboxEvent save(OutboxEvent event) {
    var mo = toMO(event);
    var saved = jpaRepository.saveAndFlush(mo);
    return toDomain(saved);
  }

  @Override
  @Transactional
  public List<OutboxEvent> findPendingForPublishing(int limit) {
    return jpaRepository.findPendingForPublishing(limit)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public void markPublished(UUID id, OffsetDateTime publishedAt) {
    jpaRepository.markPublished(id, publishedAt);
  }

  @Override
  @Transactional
  public void markFailed(UUID id, int attempts, OffsetDateTime nextAttemptAt) {
    jpaRepository.markFailed(id, attempts, nextAttemptAt);
  }

  private OutboxEventMO toMO(OutboxEvent e) {
    return OutboxEventMO.builder()
        .id(e.getId())
        .aggregateId(e.getAggregateId())
        .eventType(e.getEventType())
        .topic(e.getTopic())
        .eventKey(e.getEventKey())
        .payload(e.getPayload())
        .status(e.getStatus())
        .publicationReason(e.getPublicationReason())
        .attempts(e.getAttempts())
        .nextAttemptAt(e.getNextAttemptAt())
        .createdAt(e.getCreatedAt())
        .publishedAt(e.getPublishedAt())
        .build();
  }

  private OutboxEvent toDomain(OutboxEventMO mo) {
    return OutboxEvent.builder()
        .id(mo.getId())
        .aggregateId(mo.getAggregateId())
        .eventType(mo.getEventType())
        .topic(mo.getTopic())
        .eventKey(mo.getEventKey())
        .payload(mo.getPayload())
        .status(mo.getStatus())
        .publicationReason(mo.getPublicationReason())
        .attempts(mo.getAttempts())
        .nextAttemptAt(mo.getNextAttemptAt())
        .createdAt(mo.getCreatedAt())
        .publishedAt(mo.getPublishedAt())
        .build();
  }
}
