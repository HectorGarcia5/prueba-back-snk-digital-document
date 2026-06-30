package com.mercadona.prueba.snk.digitaldocument.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class AiCertificationData {

  String certificationId;
  String status;
  boolean valid;
  LocalDate startDate;
  LocalDate expirationDate;
  LocalDate issuedDate;
  String level;
  List<String> approvedTools;
  String issuedBy;
  String description;
}
