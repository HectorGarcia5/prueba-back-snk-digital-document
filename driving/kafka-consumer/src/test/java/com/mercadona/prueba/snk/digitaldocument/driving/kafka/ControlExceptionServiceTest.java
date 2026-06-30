package com.mercadona.prueba.snk.digitaldocument.driving.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.TopicConsumerErrorServicePort;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.employee.employee.EmployeeEventPublicKey;
import thirdparty.employee.employee.EmployeeEventPublicValue;
import thirdparty.employee.employee.ManagedGroupIds;

@ExtendWith(MockitoExtension.class)
class ControlExceptionServiceTest {

  @Mock
  private TopicConsumerErrorServicePort errorService;

  private ControlExceptionService service;

  private static final String TOPIC = "employee-topic";
  private static final int PARTITION = 0;
  private static final long OFFSET = 42L;

  @BeforeEach
  void setUp() {
    service = new ControlExceptionService(errorService);
  }

  // ---------------------------------------------------------------------------
  // controlException — happy path: key y value no nulos
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should build model from exception root cause and delegate to errorService")
  void should_build_model_from_root_cause_and_delegate() {
    EmployeeEventPublicKey key = buildKey("1", "GRP-1");
    ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record =
        new ConsumerRecord<>(TOPIC, PARTITION, OFFSET, key, null);
    var rootException = new RuntimeException("root error");

    service.controlException(key, null, rootException, record);

    ArgumentCaptor<TopicConsumerErrorModel> captor = ArgumentCaptor.forClass(TopicConsumerErrorModel.class);
    verify(errorService, times(1)).saveUpdateTopicConsumerError(captor.capture());
    var model = captor.getValue();
    assertThat(model.getTceTopicName()).isEqualTo(TOPIC);
    assertThat(model.getEventOffset()).isEqualTo((int) OFFSET);
    assertThat(model.getError()).isEqualTo("root error");
    assertThat(model.getDate()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // controlException — root cause traversal: excepcion encadenada
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should extract deepest root cause message from chained exception")
  void should_extract_deepest_root_cause_from_chained_exception() {
    EmployeeEventPublicKey key = buildKey("2", "GRP-2");
    ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record =
        new ConsumerRecord<>(TOPIC, PARTITION, OFFSET, key, null);

    var rootCause = new IllegalArgumentException("deep root cause");
    var middle = new RuntimeException("middle", rootCause);
    var outer = new Exception("outer", middle);

    service.controlException(key, null, outer, record);

    ArgumentCaptor<TopicConsumerErrorModel> captor = ArgumentCaptor.forClass(TopicConsumerErrorModel.class);
    verify(errorService, times(1)).saveUpdateTopicConsumerError(captor.capture());
    assertThat(captor.getValue().getError()).isEqualTo("deep root cause");
  }

  // ---------------------------------------------------------------------------
  // controlException — root cause sin cadena (causa directa)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should use exception message directly when no cause chain exists")
  void should_use_exception_message_when_no_cause_chain() {
    EmployeeEventPublicKey key = buildKey("3", "GRP-3");
    ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record =
        new ConsumerRecord<>(TOPIC, PARTITION, 7L, key, null);

    var e = new RuntimeException("direct error");

    service.controlException(key, null, e, record);

    verify(errorService, times(1)).saveUpdateTopicConsumerError(
        argThat(m -> "direct error".equals(m.getError()) && m.getEventOffset() == 7));
  }

  // ---------------------------------------------------------------------------
  // serialize — key nulo → eventKey null en el model
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should set eventKey to null in model when key is null")
  void should_set_event_key_null_when_key_is_null() {
    ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record =
        new ConsumerRecord<>(TOPIC, PARTITION, OFFSET, null, null);

    service.controlException(null, null, new RuntimeException("err"), record);

    ArgumentCaptor<TopicConsumerErrorModel> captor = ArgumentCaptor.forClass(TopicConsumerErrorModel.class);
    verify(errorService, times(1)).saveUpdateTopicConsumerError(captor.capture());
    assertThat(captor.getValue().getEventKey()).isNull();
  }

  // ---------------------------------------------------------------------------
  // controlException — model lleva el topicName del record
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should set tceTopicName from ConsumerRecord topic")
  void should_set_tce_topic_name_from_consumer_record() {
    EmployeeEventPublicKey key = buildKey("5", "GRP-5");
    String customTopic = "custom-input-topic";
    ConsumerRecord<EmployeeEventPublicKey, EmployeeEventPublicValue> record =
        new ConsumerRecord<>(customTopic, PARTITION, OFFSET, key, null);

    service.controlException(key, null, new RuntimeException("x"), record);

    verify(errorService, times(1)).saveUpdateTopicConsumerError(
        argThat(m -> customTopic.equals(m.getTceTopicName())));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static EmployeeEventPublicKey buildKey(String id, String managedGroupId) {
    ManagedGroupIds mgIds = ManagedGroupIds.newBuilder().setId(managedGroupId).build();
    return EmployeeEventPublicKey.newBuilder()
        .setId(id)
        .setManagedGroupId(mgIds)
        .build();
  }
}
