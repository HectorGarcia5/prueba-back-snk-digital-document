package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.TopicConsumerErrorJpaRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers.TopicConsumerErrorMapper;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.TopicConsumerErrorMO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicConsumerErrorRepositoryAdapterTest {

  @Mock
  private TopicConsumerErrorMapper mapper;

  @Mock
  private TopicConsumerErrorJpaRepository jpaRepository;

  private TopicConsumerErrorRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new TopicConsumerErrorRepositoryAdapter(mapper, jpaRepository);
  }

  // ---------------------------------------------------------------------------
  // saveTopicConsumerError
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should map model to MO and delegate to jpaRepository.save")
  void should_map_model_and_delegate_to_jpa_save() {
    var model = buildModel("employee-topic", 10, "some error");
    var mo = buildMO("employee-topic", 10, "some error");

    when(mapper.mapDomainToEntity(model)).thenReturn(mo);

    adapter.saveTopicConsumerError(model);

    verify(mapper, times(1)).mapDomainToEntity(model);
    verify(jpaRepository, times(1)).save(mo);
  }

  @Test
  @DisplayName("Should pass the exact MO returned by mapper to jpaRepository.save")
  void should_pass_exact_mo_from_mapper_to_jpa_save() {
    var model = buildModel("topic-x", 5, "error x");
    var expectedMO = buildMO("topic-x", 5, "error x");

    when(mapper.mapDomainToEntity(any())).thenReturn(expectedMO);

    adapter.saveTopicConsumerError(model);

    ArgumentCaptor<TopicConsumerErrorMO> captor = ArgumentCaptor.forClass(TopicConsumerErrorMO.class);
    verify(jpaRepository, times(1)).save(captor.capture());
    var capturedMO = captor.getValue();
    org.assertj.core.api.Assertions.assertThat(capturedMO.getTceTopicName()).isEqualTo("topic-x");
    org.assertj.core.api.Assertions.assertThat(capturedMO.getEventOffset()).isEqualTo(5);
    org.assertj.core.api.Assertions.assertThat(capturedMO.getError()).isEqualTo("error x");
  }

  @Test
  @DisplayName("Should save model with null optional fields without throwing")
  void should_save_model_with_null_optional_fields() {
    var model = TopicConsumerErrorModel.builder()
        .tceTopicName("null-topic")
        .eventOffset(0)
        .eventKey(null)
        .eventPayload(null)
        .error(null)
        .date(null)
        .build();
    var mo = TopicConsumerErrorMO.builder()
        .tceTopicName("null-topic")
        .eventOffset(0)
        .build();

    when(mapper.mapDomainToEntity(model)).thenReturn(mo);

    adapter.saveTopicConsumerError(model);

    verify(jpaRepository, times(1)).save(mo);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static TopicConsumerErrorModel buildModel(String topic, int offset, String error) {
    return TopicConsumerErrorModel.builder()
        .tceTopicName(topic)
        .eventKey("key-1")
        .eventPayload("{\"id\":1}")
        .eventOffset(offset)
        .error(error)
        .date(LocalDateTime.of(2024, 6, 15, 10, 0, 0))
        .build();
  }

  private static TopicConsumerErrorMO buildMO(String topic, int offset, String error) {
    return TopicConsumerErrorMO.builder()
        .tceTopicName(topic)
        .eventKey("key-1")
        .eventPayload("{\"id\":1}")
        .eventOffset(offset)
        .error(error)
        .date(LocalDateTime.of(2024, 6, 15, 10, 0, 0))
        .build();
  }
}
