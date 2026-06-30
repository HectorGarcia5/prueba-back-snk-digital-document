package com.mercadona.prueba.snk.digitaldocument.driven.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class AiCertificationDataDto {

  private String certificationId;
  private String status;

  @JsonProperty("isValid")
  private boolean isValid;

  private LocalDate startDate;
  private LocalDate expirationDate;
  private LocalDate issuedDate;
  private String level;
  private List<String> approvedTools;
  private String issuedBy;
  private String description;
}
