package com.mercadona.prueba.snk.digitaldocument.domain.exception;

public class EmployeeNotFoundException extends RuntimeException {

  public EmployeeNotFoundException(String employeeId, String managedGroupId) {
    super("Employee not found [employeeId=" + employeeId + ", managedGroupId=" + managedGroupId + "]");
  }
}
