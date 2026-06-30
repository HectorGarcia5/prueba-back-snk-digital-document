package com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.TopicConsumerErrorMO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TopicConsumerErrorMapperTest {

  private TopicConsumerErrorMapper mapper;

  private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 6, 15, 10, 0, 0);

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(TopicConsumerErrorMapper.class);
  }

  // ---------------------------------------------------------------------------
  // mapDomainToEntity — happy path
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should map all fields from model to MO, ignoring codId")
  void should_map_all_fields_from_model_to_mo_ignoring_cod_id() {
    var model = TopicConsumerErrorModel.builder()
        .codId(99L)
        .tceTopicName("employee-topic")
        .eventKey("key-abc")
        .eventPayload("{\"id\":1}")
        .eventOffset(7)
        .error("NullPointerException: something went wrong")
        .date(FIXED_DATE)
        .build();

    TopicConsumerErrorMO mo = mapper.mapDomainToEntity(model);

    assertThat(mo.getCodId()).isNull();
    assertThat(mo.getTceTopicName()).isEqualTo("employee-topic");
    assertThat(mo.getEventKey()).isEqualTo("key-abc");
    assertThat(mo.getEventPayload()).isEqualTo("{\"id\":1}");
    assertThat(mo.getEventOffset()).isEqualTo(7);
    assertThat(mo.getError()).isEqualTo("NullPointerException: something went wrong");
    assertThat(mo.getDate()).isEqualTo(FIXED_DATE);
  }

  // ---------------------------------------------------------------------------
  // mapDomainToEntity — null model
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return null when model is null")
  void should_return_null_when_model_is_null() {
    TopicConsumerErrorMO mo = mapper.mapDomainToEntity(null);

    assertThat(mo).isNull();
  }

  // ---------------------------------------------------------------------------
  // mapDomainToEntity — optional fields null
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should map correctly when optional fields are null")
  void should_map_correctly_when_optional_fields_are_null() {
    var model = TopicConsumerErrorModel.builder()
        .tceTopicName("empty-topic")
        .eventKey(null)
        .eventPayload(null)
        .eventOffset(0)
        .error(null)
        .date(null)
        .build();

    TopicConsumerErrorMO mo = mapper.mapDomainToEntity(model);

    assertThat(mo.getCodId()).isNull();
    assertThat(mo.getTceTopicName()).isEqualTo("empty-topic");
    assertThat(mo.getEventKey()).isNull();
    assertThat(mo.getEventPayload()).isNull();
    assertThat(mo.getEventOffset()).isZero();
    assertThat(mo.getError()).isNull();
    assertThat(mo.getDate()).isNull();
  }

  // ---------------------------------------------------------------------------
  // mapDomainToEntity — codId is always ignored (even if model has one)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should always set codId to null regardless of model value")
  void should_always_set_cod_id_null_regardless_of_model_value() {
    var modelWithId = TopicConsumerErrorModel.builder()
        .codId(1000L)
        .tceTopicName("topic")
        .eventOffset(1)
        .build();

    var modelWithoutId = TopicConsumerErrorModel.builder()
        .tceTopicName("topic")
        .eventOffset(1)
        .build();

    assertThat(mapper.mapDomainToEntity(modelWithId).getCodId()).isNull();
    assertThat(mapper.mapDomainToEntity(modelWithoutId).getCodId()).isNull();
  }
}
