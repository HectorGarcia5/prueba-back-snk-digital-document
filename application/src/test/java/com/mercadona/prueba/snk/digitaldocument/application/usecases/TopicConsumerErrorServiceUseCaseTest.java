package com.mercadona.prueba.snk.digitaldocument.application.usecases;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.TopicConsumerErrorRepositoryPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicConsumerErrorServiceUseCaseTest {

  @Mock
  private TopicConsumerErrorRepositoryPort repository;

  private TopicConsumerErrorServiceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new TopicConsumerErrorServiceUseCase(repository);
  }

  // ---------------------------------------------------------------------------
  // saveUpdateTopicConsumerError
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should delegate saveTopicConsumerError to repository when model is valid")
  void should_delegate_to_repository_when_model_is_valid() {
    var model = buildModel("my-topic", 42, "some error");

    useCase.saveUpdateTopicConsumerError(model);

    verify(repository, times(1)).saveTopicConsumerError(model);
  }

  @Test
  @DisplayName("Should delegate to repository with null error message without throwing")
  void should_delegate_to_repository_when_error_message_is_null() {
    var model = TopicConsumerErrorModel.builder()
        .tceTopicName("my-topic")
        .eventOffset(0)
        .error(null)
        .date(LocalDateTime.of(2024, 6, 15, 10, 0, 0))
        .build();

    useCase.saveUpdateTopicConsumerError(model);

    verify(repository, times(1)).saveTopicConsumerError(model);
  }

  @Test
  @DisplayName("Should delegate to repository with offset zero")
  void should_delegate_to_repository_when_offset_is_zero() {
    var model = buildModel("topic-zero", 0, "zero offset error");

    useCase.saveUpdateTopicConsumerError(model);

    verify(repository, times(1)).saveTopicConsumerError(model);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static TopicConsumerErrorModel buildModel(String topicName, int offset, String error) {
    return TopicConsumerErrorModel.builder()
        .tceTopicName(topicName)
        .eventKey("key-1")
        .eventPayload("{\"id\":1}")
        .eventOffset(offset)
        .error(error)
        .date(LocalDateTime.of(2024, 6, 15, 10, 0, 0))
        .build();
  }
}
