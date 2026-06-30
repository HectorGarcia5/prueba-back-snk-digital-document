package com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers;

import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class DigitalDocumentMapper {

  @Mapping(target = "empEmployeeId",    source = "employeeData.employeeId")
  @Mapping(target = "empManagedGroupId", source = "employeeData.managedGroupId")
  public abstract DigitalDocumentMO toMO(DigitalDocument domain);

  @Mapping(target = "employeeData", expression = "java(toEmployeeData(mo))")
  public abstract DigitalDocument toDomain(DigitalDocumentMO mo);

  protected EmployeeData toEmployeeData(DigitalDocumentMO mo) {
    if (mo.getEmpEmployeeId() == null) {
      return null;
    }
    return EmployeeData.builder()
        .employeeId(mo.getEmpEmployeeId())
        .managedGroupId(mo.getEmpManagedGroupId())
        .build();
  }
}
