package com.mercadona.prueba.snk.digitaldocument.domain;

import lombok.Builder;
import lombok.Value;

/** Datos del empleado obtenidos de la API externa cardgenerator (puerto 8081). */
@Value
@Builder
public class EmployeeData {

  String employeeId;
  String managedGroupId;
  String fullName;
  String jobFunction;
  String department;
  String email;
  String phoneExtension;
  String location;
  AiCertificationData certification;
}
