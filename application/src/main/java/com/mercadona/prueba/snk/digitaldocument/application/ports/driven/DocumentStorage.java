package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import com.mercadona.prueba.snk.digitaldocument.application.model.StorageMetadata;

import java.util.Optional;
import java.util.UUID;

public interface DocumentStorage {

  /**
   * Stores the PDF content in the bucket.
   * Idempotent: re-uploading with the same documentId overwrites with identical content.
   *
   * @return the storageKey used (e.g. "employee-documents/{documentId}.pdf")
   */
  String store(UUID documentId, byte[] content, String checksum, String contentType);

  boolean exists(UUID documentId);

  Optional<StorageMetadata> findMetadata(UUID documentId);
}
