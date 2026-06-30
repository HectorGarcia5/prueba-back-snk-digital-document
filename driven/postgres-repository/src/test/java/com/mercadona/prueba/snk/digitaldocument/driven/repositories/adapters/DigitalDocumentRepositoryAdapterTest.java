package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.DocumentAlreadyExistsException;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.DigitalDocumentJpaRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers.DigitalDocumentMapper;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DigitalDocumentRepositoryAdapterTest {

  @Mock
  private DigitalDocumentJpaRepository jpaRepository;

  @Mock
  private DigitalDocumentMapper mapper;

  private DigitalDocumentRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new DigitalDocumentRepositoryAdapter(jpaRepository, mapper);
  }

  // ---------------------------------------------------------------------------
  // save
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should delegate save to jpaRepository and map result back to domain")
  void should_save_and_return_domain_document() {
    var document = buildDocument("EMP-001", "GRP-001");
    var mo = buildMO("EMP-001", "GRP-001");
    var savedMO = buildMO("EMP-001", "GRP-001");
    var expected = buildDocument("EMP-001", "GRP-001");

    when(mapper.toMO(document)).thenReturn(mo);
    when(jpaRepository.saveAndFlush(mo)).thenReturn(savedMO);
    when(mapper.toDomain(savedMO)).thenReturn(expected);

    var result = adapter.save(document);

    assertThat(result).isEqualTo(expected);
    verify(mapper, times(1)).toMO(document);
    verify(jpaRepository, times(1)).saveAndFlush(mo);
    verify(mapper, times(1)).toDomain(savedMO);
  }

  @Test
  @DisplayName("Should throw DocumentAlreadyExistsException when jpaRepository throws DataIntegrityViolationException")
  void should_throw_document_already_exists_when_duplicate() {
    var document = buildDocument("EMP-DUP", "GRP-DUP");
    var mo = buildMO("EMP-DUP", "GRP-DUP");

    when(mapper.toMO(document)).thenReturn(mo);
    when(jpaRepository.saveAndFlush(mo)).thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> adapter.save(document))
        .isInstanceOf(DocumentAlreadyExistsException.class);
  }

  // ---------------------------------------------------------------------------
  // findById
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return mapped domain document when found by id")
  void should_find_document_by_id() {
    var id = UUID.randomUUID();
    var mo = buildMO("EMP-001", "GRP-001");
    var expected = buildDocument("EMP-001", "GRP-001");

    when(jpaRepository.findById(id)).thenReturn(Optional.of(mo));
    when(mapper.toDomain(mo)).thenReturn(expected);

    var result = adapter.findById(id);

    assertThat(result).isPresent().contains(expected);
  }

  @Test
  @DisplayName("Should return empty when document not found by id")
  void should_return_empty_when_document_not_found_by_id() {
    var id = UUID.randomUUID();
    when(jpaRepository.findById(id)).thenReturn(Optional.empty());

    var result = adapter.findById(id);

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // findByEmployeeIdAndManagedGroupId
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return mapped domain document when found by employeeId and managedGroupId")
  void should_find_document_by_employee_id_and_managed_group_id() {
    var mo = buildMO("EMP-002", "GRP-002");
    var expected = buildDocument("EMP-002", "GRP-002");

    when(jpaRepository.findByEmployeeIdAndManagedGroupId("EMP-002", "GRP-002")).thenReturn(Optional.of(mo));
    when(mapper.toDomain(mo)).thenReturn(expected);

    var result = adapter.findByEmployeeIdAndManagedGroupId("EMP-002", "GRP-002");

    assertThat(result).isPresent().contains(expected);
  }

  @Test
  @DisplayName("Should return empty when employee document not found by employeeId and managedGroupId")
  void should_return_empty_when_employee_document_not_found() {
    when(jpaRepository.findByEmployeeIdAndManagedGroupId("EMP-NONE", "GRP-NONE")).thenReturn(Optional.empty());

    var result = adapter.findByEmployeeIdAndManagedGroupId("EMP-NONE", "GRP-NONE");

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // findByStatus
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should delegate findByStatus with correct PageRequest and map results")
  void should_find_documents_by_status_with_pagination() {
    var mo1 = buildMO("EMP-1", "GRP-1");
    var mo2 = buildMO("EMP-2", "GRP-2");
    var domain1 = buildDocument("EMP-1", "GRP-1");
    var domain2 = buildDocument("EMP-2", "GRP-2");

    when(jpaRepository.findByStatus(eq(DocumentStatus.PENDING), eq(PageRequest.of(0, 2))))
        .thenReturn(List.of(mo1, mo2));
    when(mapper.toDomain(mo1)).thenReturn(domain1);
    when(mapper.toDomain(mo2)).thenReturn(domain2);

    var result = adapter.findByStatus(DocumentStatus.PENDING, 0, 2);

    assertThat(result).hasSize(2).containsExactly(domain1, domain2);
    verify(jpaRepository, times(1)).findByStatus(DocumentStatus.PENDING, PageRequest.of(0, 2));
  }

  // ---------------------------------------------------------------------------
  // countByStatus
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should delegate countByStatus and return count")
  void should_count_documents_by_status() {
    when(jpaRepository.countByStatus(DocumentStatus.PENDING)).thenReturn(3L);

    var count = adapter.countByStatus(DocumentStatus.PENDING);

    assertThat(count).isEqualTo(3L);
    verify(jpaRepository, times(1)).countByStatus(DocumentStatus.PENDING);
  }

  // ---------------------------------------------------------------------------
  // lockById
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return mapped domain document when lock acquired by id")
  void should_lock_document_by_id() {
    var id = UUID.randomUUID();
    var mo = buildMO("EMP-LOCK", "GRP-LOCK");
    var expected = buildDocument("EMP-LOCK", "GRP-LOCK");

    when(jpaRepository.findByIdWithLock(id)).thenReturn(Optional.of(mo));
    when(mapper.toDomain(mo)).thenReturn(expected);

    var result = adapter.lockById(id);

    assertThat(result).isPresent().contains(expected);
    verify(jpaRepository, times(1)).findByIdWithLock(id);
  }

  @Test
  @DisplayName("Should return empty when locking a non-existent document")
  void should_return_empty_lock_when_document_not_found() {
    var id = UUID.randomUUID();
    when(jpaRepository.findByIdWithLock(id)).thenReturn(Optional.empty());

    var result = adapter.lockById(id);

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // findFailedRetryable
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should delegate findFailedRetryable with correct limit and maxRetries")
  void should_find_failed_retryable_documents() {
    var mo = buildFailedMO("EMP-RETRY", "GRP-RETRY");
    var expected = buildFailedDocument("EMP-RETRY", "GRP-RETRY");

    when(jpaRepository.findFailedRetryable(eq(PageRequest.of(0, 10)), eq(DocumentStatus.FAILED), eq(3), any()))
        .thenReturn(List.of(mo));
    when(mapper.toDomain(mo)).thenReturn(expected);

    var result = adapter.findFailedRetryable(10, 3);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(DocumentStatus.FAILED);
    verify(jpaRepository, times(1)).findFailedRetryable(eq(PageRequest.of(0, 10)), eq(DocumentStatus.FAILED), eq(3), any());
  }

  @Test
  @DisplayName("Should return empty list when no retryable documents found")
  void should_return_empty_list_when_no_retryable_documents() {
    when(jpaRepository.findFailedRetryable(any(), any(), anyInt(), any())).thenReturn(List.of());

    var result = adapter.findFailedRetryable(10, 3);

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static DigitalDocument buildDocument(String employeeId, String managedGroupId) {
    return DigitalDocument.builder()
        .id(UUID.randomUUID())
        .employeeId(employeeId)
        .managedGroupId(managedGroupId)
        .status(DocumentStatus.PENDING)
        .retryCount(0)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }

  private static DigitalDocument buildFailedDocument(String employeeId, String managedGroupId) {
    return DigitalDocument.builder()
        .id(UUID.randomUUID())
        .employeeId(employeeId)
        .managedGroupId(managedGroupId)
        .status(DocumentStatus.FAILED)
        .failedStep(FailedStep.ENRICHMENT)
        .retryCount(0)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }

  private static DigitalDocumentMO buildMO(String employeeId, String managedGroupId) {
    return DigitalDocumentMO.builder()
        .id(UUID.randomUUID())
        .employeeId(employeeId)
        .managedGroupId(managedGroupId)
        .status(DocumentStatus.PENDING)
        .retryCount(0)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }

  private static DigitalDocumentMO buildFailedMO(String employeeId, String managedGroupId) {
    return DigitalDocumentMO.builder()
        .id(UUID.randomUUID())
        .employeeId(employeeId)
        .managedGroupId(managedGroupId)
        .status(DocumentStatus.FAILED)
        .failedStep(FailedStep.ENRICHMENT)
        .retryCount(0)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }
}
