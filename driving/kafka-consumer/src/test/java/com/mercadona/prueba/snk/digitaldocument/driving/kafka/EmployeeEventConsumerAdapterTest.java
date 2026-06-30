package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ProcessDigitalDocumentPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ReceiveEmployeeEventPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.RepublishDigitalDocumentPort;
import com.mercadona.prueba.snk.digitaldocument.application.usecases.ReceiveEmployeeEventResponse;
import com.mercadona.prueba.snk.digitaldocument.application.usecases.ReceiveEmployeeEventResult;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import thirdparty.employee.employee.EmployeeEventPublicKey;
import thirdparty.employee.employee.EmployeeEventPublicValue;
import thirdparty.employee.employee.ManagedGroupIds;

@ExtendWith(MockitoExtension.class)
class EmployeeEventConsumerAdapterTest {

  private static final String TOPIC    = "employee-topic";
  private static final String GROUP_ID = "snk-digital-document-group";

  @Mock
  private ReceiveEmployeeEventPort receiveUseCase;

  @Mock
  private ProcessDigitalDocumentPort processUseCase;

  @Mock
  private RepublishDigitalDocumentPort republishUseCase;

  @Mock
  private ControlExceptionService controlExceptionService;

  @Mock
  private Acknowledgment ack;

  private EmployeeEventConsumerAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new EmployeeEventConsumerAdapter(
        TOPIC, GROUP_ID,
        receiveUseCase, processUseCase, republishUseCase, controlExceptionService);
  }

  // ---------------------------------------------------------------------------
  // consume — null record
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should ack and return immediately when consumerRecord is null")
  void should_ack_and_return_when_consumer_record_is_null() {
    adapter.consume(null, ack);

    verify(ack, times(1)).acknowledge();
    verify(receiveUseCase, never()).receive(any(), any());
    verify(controlExceptionService, never()).controlException(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should ack and return immediately when consumerRecord key is null")
  void should_ack_and_return_when_consumer_record_key_is_null() {
    ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record =
        new ConsumerRecord<>(TOPIC, 0, 0L, null, null);

    adapter.consume(record, ack);

    verify(ack, times(1)).acknowledge();
    verify(receiveUseCase, never()).receive(any(), any());
    verify(controlExceptionService, never()).controlException(any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // consume — CREATED result
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should call processUseCase and ack when result is CREATED")
  void should_call_process_and_ack_when_result_is_created() {
    var documentId = UUID.randomUUID();
    var key = buildKey("111", "10");
    var record = buildRecord(key);

    when(receiveUseCase.receive("111", "10"))
        .thenReturn(new ReceiveEmployeeEventResponse(ReceiveEmployeeEventResult.CREATED, documentId));

    adapter.consume(record, ack);

    verify(processUseCase, times(1)).process(documentId);
    verify(ack, times(1)).acknowledge();
    verify(republishUseCase, never()).republish(any(), any(), any());
    verify(controlExceptionService, never()).controlException(any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // consume — DUPLICATE_READY_TO_REPUBLISH result
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should call republishUseCase and ack when result is DUPLICATE_READY_TO_REPUBLISH")
  void should_call_republish_and_ack_when_result_is_duplicate_ready_to_republish() {
    var documentId = UUID.randomUUID();
    var key = buildKey("222", "20");
    var record = buildRecord(key);

    when(receiveUseCase.receive("222", "20"))
        .thenReturn(new ReceiveEmployeeEventResponse(
            ReceiveEmployeeEventResult.DUPLICATE_READY_TO_REPUBLISH, documentId));

    adapter.consume(record, ack);

    verify(republishUseCase, times(1)).republish(documentId, "222", "20");
    verify(ack, times(1)).acknowledge();
    verify(processUseCase, never()).process(any());
    verify(controlExceptionService, never()).controlException(any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // consume — DUPLICATE_IN_PROGRESS result
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should only ack when result is DUPLICATE_IN_PROGRESS")
  void should_only_ack_when_result_is_duplicate_in_progress() {
    var documentId = UUID.randomUUID();
    var key = buildKey("333", "30");
    var record = buildRecord(key);

    when(receiveUseCase.receive("333", "30"))
        .thenReturn(new ReceiveEmployeeEventResponse(
            ReceiveEmployeeEventResult.DUPLICATE_IN_PROGRESS, documentId));

    adapter.consume(record, ack);

    verify(ack, times(1)).acknowledge();
    verify(processUseCase, never()).process(any());
    verify(republishUseCase, never()).republish(any(), any(), any());
    verify(controlExceptionService, never()).controlException(any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // consume — DUPLICATE_FAILED result
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should only ack when result is DUPLICATE_FAILED")
  void should_only_ack_when_result_is_duplicate_failed() {
    var documentId = UUID.randomUUID();
    var key = buildKey("444", "40");
    var record = buildRecord(key);

    when(receiveUseCase.receive("444", "40"))
        .thenReturn(new ReceiveEmployeeEventResponse(
            ReceiveEmployeeEventResult.DUPLICATE_FAILED, documentId));

    adapter.consume(record, ack);

    verify(ack, times(1)).acknowledge();
    verify(processUseCase, never()).process(any());
    verify(republishUseCase, never()).republish(any(), any(), any());
    verify(controlExceptionService, never()).controlException(any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // consume — exception path: controlExceptionService + ack (no nack)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should call controlExceptionService and ack (not nack) when receiveUseCase throws")
  void should_call_control_exception_service_and_ack_when_receive_throws() {
    var key = buildKey("555", "50");
    var record = buildRecord(key);
    var ex = new RuntimeException("processing error");

    doThrow(ex).when(receiveUseCase).receive("555", "50");

    adapter.consume(record, ack);

    verify(controlExceptionService, times(1))
        .controlException(eq(key), eq(null), eq(ex), eq(record));
    verify(ack, times(1)).acknowledge();
    verify(processUseCase, never()).process(any());
    verify(republishUseCase, never()).republish(any(), any(), any());
  }

  @Test
  @DisplayName("Should call controlExceptionService and ack when processUseCase throws")
  void should_call_control_exception_service_and_ack_when_process_throws() {
    var documentId = UUID.randomUUID();
    var key = buildKey("666", "60");
    var record = buildRecord(key);
    var ex = new RuntimeException("pdf error");

    when(receiveUseCase.receive("666", "60"))
        .thenReturn(new ReceiveEmployeeEventResponse(ReceiveEmployeeEventResult.CREATED, documentId));
    doThrow(ex).when(processUseCase).process(documentId);

    adapter.consume(record, ack);

    verify(controlExceptionService, times(1))
        .controlException(eq(key), eq(null), eq(ex), eq(record));
    verify(ack, times(1)).acknowledge();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static EmployeeEventPublicKey buildKey(String employeeId, String managedGroupId) {
    ManagedGroupIds mgIds = ManagedGroupIds.newBuilder()
        .setId(managedGroupId)
        .build();
    return EmployeeEventPublicKey.newBuilder()
        .setId(employeeId)
        .setManagedGroupId(mgIds)
        .build();
  }

  private static ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> buildRecord(
      EmployeeEventPublicKey key) {
    return new ConsumerRecord<>(TOPIC, 0, 0L, key, null);
  }
}
