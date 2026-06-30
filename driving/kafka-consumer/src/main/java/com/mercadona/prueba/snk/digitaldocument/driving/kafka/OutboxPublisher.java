package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadona.framework.cna.lib.kafka.template.MercadonaKafkaTemplate;
import com.mercadona.prueba.snk.digitaldocument.application.outbox.OutboxEvent;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.OutboxRepository;
import com.mercadona.prueba.snk.digitaldocument.application.services.DocumentStateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutKey;
import thirdparty.employee.employeedigitaldocument.v0.EmployeeDigitalDocumentEventRestrictedOutValue;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reads PENDING Outbox events and publishes them to the employee-digital-document topic.
 * After Kafka confirms delivery, atomically marks Outbox + Document as PUBLISHED.
 * On failure, increments attempts and schedules a retry with exponential backoff.
 */
@Slf4j
@Component
@Profile("!local")
public class OutboxPublisher {

  private static final int BATCH_SIZE = 50;
  private static final int MAX_ATTEMPTS = 5;

  private final OutboxRepository outboxRepository;
  private final DocumentStateService stateService;
  private final MercadonaKafkaTemplate<SpecificRecord, SpecificRecord> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${outbox.topic.employee-digital-document}")
  private String outputTopic;

  public OutboxPublisher(
      OutboxRepository outboxRepository,
      DocumentStateService stateService,
      MercadonaKafkaTemplate<SpecificRecord, SpecificRecord> kafkaTemplate,
      ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.stateService = stateService;
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:5000}")
  public void publishPending() {
    List<OutboxEvent> pending = outboxRepository.findPendingForPublishing(BATCH_SIZE);
    if (pending.isEmpty()) return;

    log.info("event=OUTBOX_PUBLISH_START count={}", pending.size());

    for (OutboxEvent event : pending) {
      try {
        var avroKey = buildKey(event);
        var avroValue = buildValue(event);

        kafkaTemplate.send(outputTopic, avroKey, avroValue).get();

        stateService.completePublication(event.getId(), event.getAggregateId());
        log.info("event=OUTBOX_PUBLISHED outboxId={} documentId={}", event.getId(), event.getAggregateId());

      } catch (Exception e) {
        int nextAttempts = event.getAttempts() + 1;
        long delaySeconds = Math.min(60L * (long) Math.pow(2, event.getAttempts()), 3600L);
        OffsetDateTime nextRetry = OffsetDateTime.now().plusSeconds(delaySeconds);

        outboxRepository.markFailed(event.getId(), nextAttempts, nextRetry);
        log.error("event=OUTBOX_PUBLISH_ERROR outboxId={} attempts={} nextRetry={} error={}",
            event.getId(), nextAttempts, nextRetry, e.getMessage());

        if (nextAttempts >= MAX_ATTEMPTS) {
          log.error("event=OUTBOX_MAX_ATTEMPTS_REACHED outboxId={}", event.getId());
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Avro object builders
  // NOTE: class names and exact builder API depend on the compiled employeedigitaldocumentv0:1.1.0
  // schema artifact. Verify against actual generated classes when corporate Maven is available.
  // ---------------------------------------------------------------------------

  private EmployeeDigitalDocumentEventRestrictedOutKey buildKey(OutboxEvent event) throws Exception {
    JsonNode payload = objectMapper.readTree(event.getPayload());
    return EmployeeDigitalDocumentEventRestrictedOutKey.newBuilder()
        .setEmployeeId(payload.path("employeeId").asText())
        .setManagedGroupId(payload.path("managedGroupId").asText())
        .build();
  }

  private EmployeeDigitalDocumentEventRestrictedOutValue buildValue(OutboxEvent event) throws Exception {
    JsonNode payload = objectMapper.readTree(event.getPayload());
    return EmployeeDigitalDocumentEventRestrictedOutValue.newBuilder()
        .setDigitalDocumentId(payload.path("digitalDocumentId").asText())
        .setEmployeeId(payload.path("employeeId").asText())
        .setManagedGroupId(payload.path("managedGroupId").asText())
        .build();
  }
}
