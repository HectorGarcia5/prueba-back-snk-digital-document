package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.TopicConsumerErrorRepositoryPort;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.TopicConsumerErrorJpaRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers.TopicConsumerErrorMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Transactional
public class TopicConsumerErrorRepositoryAdapter implements TopicConsumerErrorRepositoryPort {

  private final TopicConsumerErrorMapper mapper;
  private final TopicConsumerErrorJpaRepository jpaRepository;

  @Override
  public void saveTopicConsumerError(TopicConsumerErrorModel model) {
    jpaRepository.save(mapper.mapDomainToEntity(model));
  }
}
