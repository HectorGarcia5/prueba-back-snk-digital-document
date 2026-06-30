package com.mercadona.prueba.snk.digitaldocument.driven.client.adapters;

import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.CertificationClient;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.CertificationClientException;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.EmployeeNotFoundException;
import com.mercadona.prueba.snk.digitaldocument.driven.client.dto.EmployeeCertificationResponseDto;
import com.mercadona.prueba.snk.digitaldocument.driven.client.mapper.CertificationMapper;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificationApiClientAdapter implements CertificationClient {

  private final RestClient certificationRestClient;
  private final CertificationMapper mapper;

  @Retry(name = "certificationClient")
  @Override
  public EmployeeData getEmployeeCertification(String managedGroupId, String employeeId) {
    log.info("event=CERTIFICATION_CALL managedGroupId={} employeeId={}", managedGroupId, employeeId);
    try {
      var response = certificationRestClient.get()
          .uri("/managed-groups/{managedGroupId}/employees/{employeeId}/ai-certification",
              managedGroupId, employeeId)
          .retrieve()
          .body(EmployeeCertificationResponseDto.class);

      if (response == null || response.getData() == null) {
        throw new CertificationClientException("Empty response from certification API");
      }
      return mapper.toDomain(response.getData());

    } catch (HttpClientErrorException.NotFound e) {
      throw new EmployeeNotFoundException(employeeId, managedGroupId);
    } catch (HttpClientErrorException e) {
      throw new CertificationClientException(
          "Client error from certification API [status=" + e.getStatusCode() + "]", e);
    } catch (HttpServerErrorException e) {
      throw new CertificationClientException(
          "Server error from certification API [status=" + e.getStatusCode() + "]", e);
    } catch (ResourceAccessException e) {
      throw new CertificationClientException("Timeout or connection error calling certification API", e);
    }
  }
}
