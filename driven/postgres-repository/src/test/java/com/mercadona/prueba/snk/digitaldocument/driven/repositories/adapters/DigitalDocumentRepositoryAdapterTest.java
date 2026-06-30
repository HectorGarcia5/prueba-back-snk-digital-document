package com.mercadona.prueba.snk.digitaldocument.driven.repositories.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.DigitalDocumentJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DigitalDocumentRepositoryAdapterTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("digitaldocument_test")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private DigitalDocumentRepositoryAdapter adapter;

  @Autowired
  private DigitalDocumentJpaRepository jpaRepository;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void cleanDatabase() {
    jpaRepository.deleteAll();
  }

  // ---------------------------------------------------------------------------
  // Helper factory
  // ---------------------------------------------------------------------------

  private static DigitalDocument buildDocument(String employeeId, String managedGroupId) {
    return DigitalDocument.createPending(employeeId, managedGroupId);
  }

  // ---------------------------------------------------------------------------
  // 1. Persistencia y recuperación
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should save a pending document and find it by id")
  void should_save_and_find_pending_document_by_id() {
    var document = buildDocument("EMP-001", "GRP-001");

    var saved = adapter.save(document);
    var found = adapter.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getEmployeeId()).isEqualTo("EMP-001");
    assertThat(found.get().getManagedGroupId()).isEqualTo("GRP-001");
    assertThat(found.get().getStatus()).isEqualTo(DocumentStatus.PENDING);
    assertThat(found.get().getRetryCount()).isZero();
  }

  @Test
  @DisplayName("Should return empty when document not found by id")
  void should_return_empty_when_document_not_found() {
    var result = adapter.findById(UUID.randomUUID());

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // 2. Restricción única (employee_id, managed_group_id)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw DataIntegrityViolationException when saving duplicate employee document")
  void should_throw_exception_when_saving_duplicate_employee_document() {
    adapter.save(buildDocument("EMP-DUP", "GRP-DUP"));

    assertThatThrownBy(() -> adapter.save(buildDocument("EMP-DUP", "GRP-DUP")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // ---------------------------------------------------------------------------
  // 3. Búsqueda por empleado
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should find document by employeeId and managedGroupId")
  void should_find_document_by_employee_id_and_managed_group_id() {
    adapter.save(buildDocument("EMP-002", "GRP-002"));

    var found = adapter.findByEmployeeIdAndManagedGroupId("EMP-002", "GRP-002");

    assertThat(found).isPresent();
    assertThat(found.get().getEmployeeId()).isEqualTo("EMP-002");
    assertThat(found.get().getManagedGroupId()).isEqualTo("GRP-002");
  }

  @Test
  @DisplayName("Should return empty when employee document not found by employeeId and managedGroupId")
  void should_return_empty_when_employee_document_not_found() {
    var result = adapter.findByEmployeeIdAndManagedGroupId("EMP-NONE", "GRP-NONE");

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // 4. Filtrado por estado con paginación
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return paginated documents filtered by PENDING status — page 0, size 3 returns 3 of 5")
  void should_find_documents_by_status_paginated() {
    for (int i = 1; i <= 5; i++) {
      adapter.save(buildDocument("EMP-PAG-" + i, "GRP-PAG-" + i));
    }

    var page = adapter.findByStatus(DocumentStatus.PENDING, 0, 3);

    assertThat(page).hasSize(3);
    assertThat(page).allMatch(d -> d.getStatus() == DocumentStatus.PENDING);
  }

  @Test
  @DisplayName("Should count documents by status accurately")
  void should_count_documents_by_status() {
    adapter.save(buildDocument("EMP-CNT-1", "GRP-CNT-1"));
    adapter.save(buildDocument("EMP-CNT-2", "GRP-CNT-2"));
    adapter.save(buildDocument("EMP-CNT-3", "GRP-CNT-3"));

    var count = adapter.countByStatus(DocumentStatus.PENDING);

    assertThat(count).isEqualTo(3);
  }

  // ---------------------------------------------------------------------------
  // 5. Versionado optimista
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should increment version on each save")
  void should_increment_version_on_each_save() {
    // Primera persistencia: version=0
    var saved = adapter.save(buildDocument("EMP-VER", "GRP-VER"));
    assertThat(saved.getVersion()).isZero();

    // Segunda persistencia con mismo id: version en BD pasa a 1
    // Usamos el documento devuelto por el primer save (version=0 del objeto Java)
    // y comprobamos el estado en BD después del segundo commit
    adapter.save(saved);

    // Verificamos en BD que la version es ahora 1
    var afterSecondSave = adapter.findById(saved.getId()).orElseThrow();
    assertThat(afterSecondSave.getVersion()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should throw ObjectOptimisticLockingFailureException on concurrent modification of same document")
  void should_throw_on_optimistic_lock_conflict() {
    var saved = adapter.save(buildDocument("EMP-OPT", "GRP-OPT"));

    // Limpiar caché para que instanceA y instanceB se traten como entidades independientes
    entityManager.clear();

    // Instancia A: guarda con version=0, BD pasa a version=1
    var instanceA = DigitalDocument.builder()
        .id(saved.getId())
        .employeeId(saved.getEmployeeId())
        .managedGroupId(saved.getManagedGroupId())
        .status(DocumentStatus.PENDING)
        .retryCount(0)
        .createdAt(saved.getCreatedAt())
        .updatedAt(saved.getUpdatedAt())
        .version(0L)
        .build();
    adapter.save(instanceA);

    // Limpiar caché de nuevo para evitar que instanceB sea merged como la misma entidad
    entityManager.clear();

    // Instancia B: version=0 (stale) — debe lanzar conflicto optimista
    var instanceB = DigitalDocument.builder()
        .id(saved.getId())
        .employeeId(saved.getEmployeeId())
        .managedGroupId(saved.getManagedGroupId())
        .status(DocumentStatus.PENDING)
        .retryCount(0)
        .createdAt(saved.getCreatedAt())
        .updatedAt(saved.getUpdatedAt())
        .version(0L)
        .build();

    assertThatThrownBy(() -> adapter.save(instanceB))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }

  // ---------------------------------------------------------------------------
  // 6. Lock pesimista
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  @DisplayName("Should lock document by id and return it")
  void should_lock_document_by_id() {
    var saved = adapter.save(buildDocument("EMP-LOCK", "GRP-LOCK"));
    entityManager.flush();
    entityManager.clear();

    var locked = adapter.lockById(saved.getId());

    assertThat(locked).isPresent();
    assertThat(locked.get().getId()).isEqualTo(saved.getId());
  }

  @Test
  @Transactional
  @DisplayName("Should return empty when locking a non-existent document")
  void should_return_empty_lock_when_document_not_found() {
    var result = adapter.lockById(UUID.randomUUID());

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // 7. findFailedRetryable
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return FAILED retryable documents within limit and maxRetries")
  void should_find_failed_retryable_documents() {
    var saved = adapter.save(buildDocument("EMP-RETRY", "GRP-RETRY"));

    var failedDoc = DigitalDocument.builder()
        .id(saved.getId())
        .employeeId(saved.getEmployeeId())
        .managedGroupId(saved.getManagedGroupId())
        .status(DocumentStatus.FAILED)
        .failedStep(FailedStep.ENRICHMENT)
        .retryCount(0)
        .lastErrorCode("ERR-001")
        .lastErrorMessage("error")
        .createdAt(saved.getCreatedAt())
        .updatedAt(saved.getUpdatedAt())
        .version(saved.getVersion())
        .build();
    adapter.save(failedDoc);

    var retryable = adapter.findFailedRetryable(10, 3);

    assertThat(retryable).hasSize(1);
    assertThat(retryable.get(0).getStatus()).isEqualTo(DocumentStatus.FAILED);
    assertThat(retryable.get(0).getRetryCount()).isZero();
  }

  @Test
  @DisplayName("Should not return FAILED documents whose retryCount exceeds maxRetries")
  void should_not_return_failed_documents_exceeding_max_retries() {
    var saved = adapter.save(buildDocument("EMP-MAXR", "GRP-MAXR"));

    var failedDoc = DigitalDocument.builder()
        .id(saved.getId())
        .employeeId(saved.getEmployeeId())
        .managedGroupId(saved.getManagedGroupId())
        .status(DocumentStatus.FAILED)
        .failedStep(FailedStep.ENRICHMENT)
        .retryCount(5)
        .lastErrorCode("ERR-MAX")
        .lastErrorMessage("max retries exceeded")
        .createdAt(saved.getCreatedAt())
        .updatedAt(saved.getUpdatedAt())
        .version(saved.getVersion())
        .build();
    adapter.save(failedDoc);

    var retryable = adapter.findFailedRetryable(10, 3);

    assertThat(retryable).isEmpty();
  }
}
