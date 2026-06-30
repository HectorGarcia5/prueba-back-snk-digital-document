package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;

public interface CertificationClient {

  /**
   * Retrieves employee certification data from the external cardgenerator API.
   *
   * @throws com.mercadona.prueba.snk.digitaldocument.domain.exception.EmployeeNotFoundException if 404
   * @throws com.mercadona.prueba.snk.digitaldocument.domain.exception.CertificationClientException if 5xx or timeout
   */
  EmployeeData getEmployeeCertification(String managedGroupId, String employeeId);
}
