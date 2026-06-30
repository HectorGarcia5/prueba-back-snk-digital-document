package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentRepository;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.DocumentAlreadyExistsException;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.DigitalDocumentJpaRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers.DigitalDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DigitalDocumentRepositoryAdapter implements DocumentRepository {

  private final DigitalDocumentJpaRepository jpaRepository;
  private final DigitalDocumentMapper mapper;

  @Override
  public DigitalDocument save(DigitalDocument document) {
    try {
      var mo = mapper.toMO(document);
      var saved = jpaRepository.saveAndFlush(mo);
      return mapper.toDomain(saved);
    } catch (DataIntegrityViolationException e) {
      throw new DocumentAlreadyExistsException(document.getEmployeeId(), document.getManagedGroupId());
    }
  }

  @Override
  public Optional<DigitalDocument> findById(UUID id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<DigitalDocument> findByEmployeeIdAndManagedGroupId(String employeeId, String managedGroupId) {
    return jpaRepository.findByEmployeeIdAndManagedGroupId(employeeId, managedGroupId)
        .map(mapper::toDomain);
  }

  @Override
  public List<DigitalDocument> findByStatus(DocumentStatus status, int page, int size) {
    return jpaRepository.findByStatus(status, PageRequest.of(page, size))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countByStatus(DocumentStatus status) {
    return jpaRepository.countByStatus(status);
  }

  @Override
  public Optional<DigitalDocument> lockById(UUID id) {
    return jpaRepository.findByIdWithLock(id).map(mapper::toDomain);
  }

  @Override
  public List<DigitalDocument> findFailedRetryable(int limit, int maxRetries) {
    return jpaRepository.findFailedRetryable(limit, maxRetries)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
