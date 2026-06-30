package com.mercadona.prueba.snk.digitaldocument.application.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdfResult {

  byte[] content;
  String checksum;
  String contentType;
}
