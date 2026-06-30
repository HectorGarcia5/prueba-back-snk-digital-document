package com.mercadona.prueba.snk.digitaldocument.driven.repositories.models;

import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxStatus;
import com.mercadona.prueba.snk.digitaldocument.application.outbox.PublicationReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventMO {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "aggregate_id", nullable = false, updatable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false, updatable = false)
  private String eventType;

  @Column(name = "topic", nullable = false, updatable = false)
  private String topic;

  @Column(name = "event_key", nullable = false, updatable = false)
  private String eventKey;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OutboxStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "publication_reason", nullable = false, updatable = false)
  private PublicationReason publicationReason;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at")
  private OffsetDateTime nextAttemptAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "published_at")
  private OffsetDateTime publishedAt;
}
