package com.mercadona.prueba.snk.digitaldocument.domain.exception;

public class DocumentAlreadyExistsException extends RuntimeException {

  public DocumentAlreadyExistsException(String employeeId, String managedGroupId) {
    super("Document already exists [employeeId=" + employeeId + ", managedGroupId=" + managedGroupId + "]");
  }
}
