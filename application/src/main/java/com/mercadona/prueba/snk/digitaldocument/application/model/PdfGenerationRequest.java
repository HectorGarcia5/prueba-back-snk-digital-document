package com.mercadona.prueba.snk.digitaldocument.application.model;

import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PdfGenerationRequest {

  UUID documentId;
  String employeeId;
  String managedGroupId;
  EmployeeData employeeData;

  /**
   * Used as the PDF creation date metadata to ensure deterministic generation:
   * same inputs always produce the same bytes and therefore the same checksum.
   * Should be set to document.getCreatedAt().
   */
  OffsetDateTime generationDate;
}
