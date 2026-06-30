package com.mercadona.prueba.snk.digitaldocument.driven.client.mapper;

import com.mercadona.prueba.snk.digitaldocument.domain.AiCertificationData;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.driven.client.dto.AiCertificationDataDto;
import com.mercadona.prueba.snk.digitaldocument.driven.client.dto.EmployeeDataDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificationMapper {

  AiCertificationData toDomain(AiCertificationDataDto dto);

  EmployeeData toDomain(EmployeeDataDto dto);
}
