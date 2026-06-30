package com.mercadona.prueba.snk.digitaldocument.application.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class OutboxEvent {

  private static final String TOPIC = "employee-digital-document";
  private static final String EVENT_TYPE = "EmployeeDigitalDocumentCreated";

  private UUID id;
  private UUID aggregateId;
  private String eventType;
  private String topic;
  private String eventKey;
  private String payload;
  private OutboxStatus status;
  private PublicationReason publicationReason;
  private int attempts;
  private OffsetDateTime nextAttemptAt;
  private OffsetDateTime createdAt;
  private OffsetDateTime publishedAt;

  public static OutboxEvent createInitial(UUID documentId, String employeeId, String managedGroupId) {
    return create(documentId, employeeId, managedGroupId, PublicationReason.INITIAL);
  }

  public static OutboxEvent createForDuplicate(UUID documentId, String employeeId, String managedGroupId) {
    return create(documentId, employeeId, managedGroupId, PublicationReason.DUPLICATE_EVENT);
  }

  public static OutboxEvent createForRetry(UUID documentId, String employeeId, String managedGroupId) {
    return create(documentId, employeeId, managedGroupId, PublicationReason.MANUAL_RETRY);
  }

  private static OutboxEvent create(UUID documentId, String employeeId,
      String managedGroupId, PublicationReason reason) {
    return OutboxEvent.builder()
        .id(UUID.randomUUID())
        .aggregateId(documentId)
        .eventType(EVENT_TYPE)
        .topic(TOPIC)
        .eventKey(documentId.toString())
        .payload(buildPayload(documentId, employeeId, managedGroupId))
        .status(OutboxStatus.PENDING)
        .publicationReason(reason)
        .attempts(0)
        .createdAt(OffsetDateTime.now())
        .build();
  }

  /** Payload uses digitalDocumentId (Avro field name) per schema definition. */
  private static String buildPayload(UUID documentId, String employeeId, String managedGroupId) {
    return "{\"digitalDocumentId\":\"" + documentId
        + "\",\"employeeId\":\"" + employeeId
        + "\",\"managedGroupId\":\"" + managedGroupId + "\"}";
  }
}
