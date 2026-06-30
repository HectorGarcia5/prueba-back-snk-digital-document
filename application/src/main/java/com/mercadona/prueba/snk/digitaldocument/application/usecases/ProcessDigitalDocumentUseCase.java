package com.mercadona.prueba.snk.digitaldocument.application.usecases;

import com.mercadona.prueba.snk.digitaldocument.application.model.PdfGenerationRequest;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.CertificationClient;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentRepository;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.DocumentStorage;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driven.PdfGenerator;
import com.mercadona.prueba.snk.digitaldocument.application.ports.driving.ProcessDigitalDocumentPort;
import com.mercadona.prueba.snk.digitaldocument.application.services.DocumentStateService;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.FailedStep;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.CertificationClientException;
import com.mercadona.prueba.snk.digitaldocument.domain.exception.EmployeeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates the document processing pipeline one step at a time.
 * Not @Transactional: external calls (certification API, S3) must never
 * hold a DB transaction open. Each state transition delegates to
 * DocumentStateService which handles its own atomic transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDigitalDocumentUseCase implements ProcessDigitalDocumentPort {

  private final DocumentRepository documentRepository;
  private final CertificationClient certificationClient;
  private final PdfGenerator pdfGenerator;
  private final DocumentStorage documentStorage;
  private final DocumentStateService stateService;

  @Override
  public void process(UUID documentId) {
    var document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found [id=" + documentId + "]"));

    log.info("event=PROCESS_START documentId={} status={}", documentId, document.getStatus());

    switch (document.getStatus()) {
      case PENDING       -> enrich(document);
      case ENRICHED      -> generatePdf(document);
      case PDF_GENERATED -> store(document);
      case STORED        -> log.info("event=PROCESS_STORED_AWAITING_OUTBOX documentId={}", documentId);
      case PUBLISHED     -> log.info("event=PROCESS_ALREADY_PUBLISHED documentId={}", documentId);
      case FAILED        -> log.warn("event=PROCESS_SKIPPED_FAILED documentId={} failedStep={}",
                              documentId, document.getFailedStep());
    }
  }

  // ---------------------------------------------------------------------------
  // Steps
  // ---------------------------------------------------------------------------

  private void enrich(DigitalDocument document) {
    log.info("event=ENRICHMENT_START documentId={}", document.getId());
    try {
      var data = certificationClient.getEmployeeCertification(
          document.getManagedGroupId(), document.getEmployeeId());
      stateService.transitionToEnriched(document, data);
      log.info("event=ENRICHMENT_OK documentId={}", document.getId());
    } catch (EmployeeNotFoundException e) {
      log.warn("event=ENRICHMENT_NOT_FOUND documentId={}", document.getId());
      stateService.transitionToFailed(document, FailedStep.ENRICHMENT,
          "EMPLOYEE_NOT_FOUND", e.getMessage());
    } catch (CertificationClientException e) {
      log.error("event=ENRICHMENT_ERROR documentId={}", document.getId());
      stateService.transitionToFailed(document, FailedStep.ENRICHMENT,
          "ENRICHMENT_SERVICE_ERROR", "Certification service unavailable");
    }
  }

  private void generatePdf(DigitalDocument document) {
    log.info("event=PDF_GENERATION_START documentId={}", document.getId());
    try {
      var result = pdfGenerator.generate(buildPdfRequest(document));
      stateService.transitionToPdfGenerated(document, result.getChecksum());
      log.info("event=PDF_GENERATION_OK documentId={} checksum={}", document.getId(), result.getChecksum());
    } catch (Exception e) {
      log.error("event=PDF_GENERATION_ERROR documentId={}", document.getId());
      stateService.transitionToFailed(document, FailedStep.PDF_GENERATION,
          "PDF_GENERATION_ERROR", "PDF generation failed");
    }
  }

  private void store(DigitalDocument document) {
    log.info("event=STORAGE_START documentId={}", document.getId());
    try {
      // Deterministic regeneration: same employeeData + createdAt → same bytes → same checksum
      var pdfResult = pdfGenerator.generate(buildPdfRequest(document));

      if (document.getChecksum() != null && !pdfResult.getChecksum().equals(document.getChecksum())) {
        log.error("event=STORAGE_CHECKSUM_MISMATCH documentId={}", document.getId());
        stateService.transitionToFailed(document, FailedStep.STORAGE,
            "CHECKSUM_MISMATCH", "Regenerated PDF checksum does not match stored checksum");
        return;
      }

      // S3 upload — outside transaction
      var storageKey = documentStorage.store(
          document.getId(), pdfResult.getContent(), pdfResult.getChecksum(), pdfResult.getContentType());

      // STORED + Outbox creation in one atomic transaction
      stateService.transitionToStored(document, storageKey);
      log.info("event=STORAGE_OK documentId={} storageKey={}", document.getId(), storageKey);

    } catch (Exception e) {
      log.error("event=STORAGE_ERROR documentId={}", document.getId());
      stateService.transitionToFailed(document, FailedStep.STORAGE,
          "STORAGE_ERROR", "Storage operation failed");
    }
  }

  private PdfGenerationRequest buildPdfRequest(DigitalDocument document) {
    return PdfGenerationRequest.builder()
        .documentId(document.getId())
        .employeeId(document.getEmployeeId())
        .managedGroupId(document.getManagedGroupId())
        .employeeData(document.getEmployeeData())
        .generationDate(document.getCreatedAt())
        .build();
  }
}
