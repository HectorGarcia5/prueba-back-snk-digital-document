package com.mercadona.prueba.snk.digitaldocument.driven.storage;

import com.google.common.io.ByteSource;
import com.mercadona.framework.cna.lib.bucket.service.BucketService;
import com.mercadona.prueba.snk.digitaldocument.application.model.StorageMetadata;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStorageAdapter implements DocumentStorage {

  private static final String KEY_TEMPLATE = "employee-documents/%s.pdf";
  private static final String META_DOCUMENT_ID = "documentid";
  private static final String META_CHECKSUM = "checksum";

  @Value("${storage.bucket-id:employee-documents}")
  private String bucketId;

  private final BucketService bucketService;

  @Override
  public String store(UUID documentId, byte[] content, String checksum, String contentType) {
    String key = storageKey(documentId);
    log.info("event=STORAGE_UPLOAD documentId={} key={} size={}", documentId, key, content.length);

    var metadata = Map.of(
        META_DOCUMENT_ID, documentId.toString(),
        META_CHECKSUM, checksum
    );

    try {
      bucketService.upload(bucketId, ByteSource.wrap(content), key, contentType, metadata);
      log.info("event=STORAGE_UPLOADED documentId={} key={}", documentId, key);
      return key;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Error uploading document to storage [documentId=" + documentId + ", key=" + key + "]", e);
    }
  }

  @Override
  public boolean exists(UUID documentId) {
    try {
      return bucketService.isBlobExist(bucketId, storageKey(documentId));
    } catch (Exception e) {
      log.warn("event=STORAGE_EXISTS_ERROR documentId={} error={}", documentId, e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<StorageMetadata> findMetadata(UUID documentId) {
    String key = storageKey(documentId);
    try {
      var rawMeta = bucketService.getMetadata(bucketId, key);
      if (rawMeta == null || rawMeta.getUserMetaData() == null) {
        return Optional.empty();
      }
      var userMeta = rawMeta.getUserMetaData();
      return Optional.of(StorageMetadata.builder()
          .storageKey(key)
          .documentId(userMeta.getOrDefault(META_DOCUMENT_ID, null))
          .checksum(userMeta.getOrDefault(META_CHECKSUM, null))
          .build());
    } catch (Exception e) {
      log.warn("event=STORAGE_METADATA_ERROR documentId={} error={}", documentId, e.getMessage());
      return Optional.empty();
    }
  }

  private String storageKey(UUID documentId) {
    return String.format(KEY_TEMPLATE, documentId);
  }
}
