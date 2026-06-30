package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;

public interface TopicConsumerErrorRepositoryPort {

  void saveTopicConsumerError(TopicConsumerErrorModel model);
}
