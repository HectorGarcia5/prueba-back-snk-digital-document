package com.mercadona.prueba.snk.digitaldocument.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicConsumerErrorModel {

  private Long codId;
  private String tceTopicName;
  private String eventKey;
  private String eventPayload;
  private Integer eventOffset;
  private String error;
  private LocalDateTime date;
}
