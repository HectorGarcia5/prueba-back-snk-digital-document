package com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.DocumentStatus;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DigitalDocumentMapperTest {

  private DigitalDocumentMapper mapper;

  private static final UUID ID              = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final String EMPLOYEE_ID   = "EMP-MAP-001";
  private static final String GROUP_ID      = "GRP-MAP-001";
  private static final OffsetDateTime NOW   = OffsetDateTime.of(2024, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC);


  @BeforeEach
  void setUp() throws Exception {
    mapper = Mappers.getMapper(DigitalDocumentMapper.class);
    var field = DigitalDocumentMapper.class.getDeclaredField("objectMapper");
    field.setAccessible(true);
    var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    field.set(mapper, objectMapper);
  }

  // ---------------------------------------------------------------------------
  // 1. Roundtrip completo con todos los campos
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should map domain to MO and back preserving all fields")
  void should_map_domain_to_mo_and_back_with_all_fields() {
    var employeeData = EmployeeData.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(GROUP_ID)
        .build();

    var domain = DigitalDocument.builder()
        .id(ID)
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(GROUP_ID)
        .status(DocumentStatus.ENRICHED)
        .failedStep(null)
        .employeeData(employeeData)
        .storageKey("bucket/path/doc.pdf")
        .checksum("sha256-abc123")
        .retryCount(2)
        .nextRetryAt(NOW.plusSeconds(120))
        .lastErrorCode("ERR-001")
        .lastErrorMessage("some error")
        .createdAt(NOW)
        .updatedAt(NOW)
        .publishedAt(null)
        .version(3L)
        .build();

    var mo = mapper.toMO(domain);

    assertThat(mo.getId()).isEqualTo(ID);
    assertThat(mo.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
    assertThat(mo.getManagedGroupId()).isEqualTo(GROUP_ID);
    assertThat(mo.getStatus()).isEqualTo(DocumentStatus.ENRICHED);
    assertThat(mo.getFailedStep()).isNull();
    assertThat(mo.getStorageKey()).isEqualTo("bucket/path/doc.pdf");
    assertThat(mo.getChecksum()).isEqualTo("sha256-abc123");
    assertThat(mo.getRetryCount()).isEqualTo(2);
    assertThat(mo.getNextRetryAt()).isEqualTo(NOW.plusSeconds(120));
    assertThat(mo.getLastErrorCode()).isEqualTo("ERR-001");
    assertThat(mo.getLastErrorMessage()).isEqualTo("some error");
    assertThat(mo.getCreatedAt()).isEqualTo(NOW);
    assertThat(mo.getUpdatedAt()).isEqualTo(NOW);
    assertThat(mo.getPublishedAt()).isNull();
    assertThat(mo.getVersion()).isEqualTo(3L);

    var roundtrip = mapper.toDomain(mo);

    assertThat(roundtrip.getId()).isEqualTo(ID);
    assertThat(roundtrip.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
    assertThat(roundtrip.getManagedGroupId()).isEqualTo(GROUP_ID);
    assertThat(roundtrip.getStatus()).isEqualTo(DocumentStatus.ENRICHED);
    assertThat(roundtrip.getEmployeeData()).isNotNull();
    assertThat(roundtrip.getEmployeeData().getEmployeeId()).isEqualTo(EMPLOYEE_ID);
    assertThat(roundtrip.getEmployeeData().getManagedGroupId()).isEqualTo(GROUP_ID);
    assertThat(roundtrip.getStorageKey()).isEqualTo("bucket/path/doc.pdf");
    assertThat(roundtrip.getChecksum()).isEqualTo("sha256-abc123");
    assertThat(roundtrip.getRetryCount()).isEqualTo(2);
    assertThat(roundtrip.getVersion()).isEqualTo(3L);
  }

  // ---------------------------------------------------------------------------
  // 2. EmployeeData nulo → campos emp_* nulos → toDomain devuelve null
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should map null employeeData to null and reconstruct null employeeData on toDomain")
  void should_map_null_employee_data_to_null() {
    var domain = DigitalDocument.builder()
        .id(ID)
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(GROUP_ID)
        .status(DocumentStatus.PENDING)
        .employeeData(null)
        .retryCount(0)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(0L)
        .build();

    var mo = mapper.toMO(domain);

    var roundtrip = mapper.toDomain(mo);

    assertThat(roundtrip.getEmployeeData()).isNull();
  }

  // ---------------------------------------------------------------------------
  // 3. FailedStep se preserva en el roundtrip
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should preserve failedStep in roundtrip domain → MO → domain")
  void should_preserve_failed_step_in_roundtrip() {
    var domain = DigitalDocument.builder()
        .id(ID)
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(GROUP_ID)
        .status(DocumentStatus.FAILED)
        .failedStep(FailedStep.PDF_GENERATION)
        .lastErrorCode("ERR-PDF")
        .lastErrorMessage("PDF generation failed")
        .retryCount(1)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1L)
        .build();

    var mo = mapper.toMO(domain);

    assertThat(mo.getFailedStep()).isEqualTo(FailedStep.PDF_GENERATION);

    var roundtrip = mapper.toDomain(mo);

    assertThat(roundtrip.getFailedStep()).isEqualTo(FailedStep.PDF_GENERATION);
    assertThat(roundtrip.getLastErrorCode()).isEqualTo("ERR-PDF");
    assertThat(roundtrip.getLastErrorMessage()).isEqualTo("PDF generation failed");
  }
}
