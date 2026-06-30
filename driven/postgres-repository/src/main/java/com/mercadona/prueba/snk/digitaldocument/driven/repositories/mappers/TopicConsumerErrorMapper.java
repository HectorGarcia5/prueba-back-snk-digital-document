package com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers;

import com.mercadona.prueba.snk.digitaldocument.application.model.TopicConsumerErrorModel;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.TopicConsumerErrorMO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicConsumerErrorMapper {

  @Mapping(target = "codId", ignore = true)
  TopicConsumerErrorMO mapDomainToEntity(TopicConsumerErrorModel model);
}
