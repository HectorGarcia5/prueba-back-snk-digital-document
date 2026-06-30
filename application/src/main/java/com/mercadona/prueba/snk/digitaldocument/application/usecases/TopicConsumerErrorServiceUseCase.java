package com.mercadona.prueba.snk.digitaldocument.application.usecases;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.TopicConsumerErrorRepositoryPort;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.TopicConsumerErrorServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicConsumerErrorServiceUseCase implements TopicConsumerErrorServicePort {

  private final TopicConsumerErrorRepositoryPort repository;

  @Override
  public void saveUpdateTopicConsumerError(TopicConsumerErrorModel model) {
    log.warn("event=TOPIC_CONSUMER_ERROR topic={} offset={} error={}",
        model.getTceTopicName(), model.getEventOffset(), model.getError());
    repository.saveTopicConsumerError(model);
  }
}
