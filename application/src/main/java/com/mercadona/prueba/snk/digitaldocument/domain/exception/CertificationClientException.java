package com.mercadona.prueba.snk.digitaldocument.domain.exception;

public class CertificationClientException extends RuntimeException {

  public CertificationClientException(String message) {
    super(message);
  }

  public CertificationClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
