package com.mercadona.prueba.snk.digitaldocument.application.ports.driving;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;

public interface TopicConsumerErrorServicePort {

  void saveUpdateTopicConsumerError(TopicConsumerErrorModel model);
}
