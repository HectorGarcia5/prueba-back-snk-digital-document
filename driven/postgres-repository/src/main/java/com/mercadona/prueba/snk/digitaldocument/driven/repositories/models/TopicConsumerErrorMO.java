package com.mercadona.prueba.snk.digitaldocument.driven.repositories.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_consumer_error")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicConsumerErrorMO {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cod_id")
  private Long codId;

  @Column(name = "tce_topic_name")
  private String tceTopicName;

  @Column(name = "event_key", columnDefinition = "text")
  private String eventKey;

  @Column(name = "event_payload", columnDefinition = "text")
  private String eventPayload;

  @Column(name = "event_offset")
  private Integer eventOffset;

  @Column(name = "error")
  private String error;

  @Column(name = "creation_date")
  private LocalDateTime date;
}
