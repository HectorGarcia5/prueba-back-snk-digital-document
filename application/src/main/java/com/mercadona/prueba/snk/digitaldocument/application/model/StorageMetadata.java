package com.mercadona.prueba.snk.digitaldocument.application.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StorageMetadata {

  String storageKey;
  String documentId;
  String checksum;
}
