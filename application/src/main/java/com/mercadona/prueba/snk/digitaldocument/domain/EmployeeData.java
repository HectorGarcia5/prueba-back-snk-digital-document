package com.mercadona.prueba.snk.digitaldocument.domain;

import lombok.Builder;
import lombok.Value;

/**
 * Datos del empleado obtenidos de la API externa cardgenerator.
 * Los campos concretos se completarán en Fase 5 tras inspeccionar el contrato Swagger
 * de oromerji/prueba-rrhh-cardgenerator (puerto 8081).
 */
@Value
@Builder
public class EmployeeData {

  String employeeId;
  String managedGroupId;
}
