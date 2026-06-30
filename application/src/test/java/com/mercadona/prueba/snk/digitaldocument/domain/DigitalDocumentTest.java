package com.mercadona.prueba.snk.digitaldocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.mercadona.prueba.snk.digitaldocument.domain.exception.DigitalDocumentStateException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class DigitalDocumentTest {

  private static final String EMPLOYEE_ID     = "EMP-001";
  private static final String MANAGED_GROUP_ID = "GRP-001";
  private static final String CHECKSUM         = "sha256-abc123";
  private static final String STORAGE_KEY      = "bucket/path/doc.pdf";
  private static final String ERROR_CODE       = "ERR-500";
  private static final String ERROR_MESSAGE    = "Internal error";

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static DigitalDocument buildDocumentWithStatus(DocumentStatus status) {
    return DigitalDocument.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .status(status)
        .retryCount(0)
        .version(0L)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  private static DigitalDocument buildFailedDocument(FailedStep failedStep) {
    return DigitalDocument.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .status(DocumentStatus.FAILED)
        .failedStep(failedStep)
        .lastErrorCode(ERROR_CODE)
        .lastErrorMessage(ERROR_MESSAGE)
        .retryCount(0)
        .version(0L)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  private static DigitalDocument buildEnrichedDocument() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);
    doc.markEnriched(EmployeeData.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .build());
    return doc;
  }

  private static DigitalDocument buildPdfGeneratedDocument() {
    var doc = buildEnrichedDocument();
    doc.markPdfGenerated(CHECKSUM);
    return doc;
  }

  private static DigitalDocument buildStoredDocument() {
    var doc = buildPdfGeneratedDocument();
    doc.markStored(STORAGE_KEY);
    return doc;
  }

  // ---------------------------------------------------------------------------
  // 1. createPending
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should generate a non-null UUID when creating a pending document")
  void should_generateNonNullUuid_when_createPending() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);

    assertThat(doc.getId()).isNotNull();
  }

  @Test
  @DisplayName("Should set status PENDING when creating a pending document")
  void should_setStatusPending_when_createPending() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PENDING);
  }

  @Test
  @DisplayName("Should set retryCount=0 and version=0 when creating a pending document")
  void should_setRetryCountAndVersionZero_when_createPending() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);

    assertThat(doc.getRetryCount()).isZero();
    assertThat(doc.getVersion()).isZero();
  }

  @Test
  @DisplayName("Should set non-null and approximately equal createdAt and updatedAt when creating a pending document")
  void should_setCreatedAtAndUpdatedAtNonNull_when_createPending() {
    var before = OffsetDateTime.now();
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);
    var after = OffsetDateTime.now();

    assertThat(doc.getCreatedAt()).isNotNull();
    assertThat(doc.getUpdatedAt()).isNotNull();
    assertThat(doc.getCreatedAt()).isCloseTo(doc.getUpdatedAt(), within(1, ChronoUnit.SECONDS));
    assertThat(doc.getCreatedAt()).isAfterOrEqualTo(before);
    assertThat(doc.getCreatedAt()).isBeforeOrEqualTo(after);
  }

  @Test
  @DisplayName("Should assign employeeId and managedGroupId when creating a pending document")
  void should_assignEmployeeIdAndManagedGroupId_when_createPending() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);

    assertThat(doc.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
    assertThat(doc.getManagedGroupId()).isEqualTo(MANAGED_GROUP_ID);
  }

  // ---------------------------------------------------------------------------
  // 2. Happy-path transitions
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should transition to ENRICHED and assign employeeData when markEnriched is called on PENDING document")
  void should_transitionToEnriched_when_markEnrichedCalledOnPendingDocument() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);
    var employeeData = EmployeeData.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .build();
    var updatedAtBefore = doc.getUpdatedAt();

    doc.markEnriched(employeeData);

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.ENRICHED);
    assertThat(doc.getEmployeeData()).isEqualTo(employeeData);
    assertThat(doc.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
  }

  @Test
  @DisplayName("Should transition to PDF_GENERATED and assign checksum when markPdfGenerated is called on ENRICHED document")
  void should_transitionToPdfGenerated_when_markPdfGeneratedCalledOnEnrichedDocument() {
    var doc = buildEnrichedDocument();
    var updatedAtBefore = doc.getUpdatedAt();

    doc.markPdfGenerated(CHECKSUM);

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PDF_GENERATED);
    assertThat(doc.getChecksum()).isEqualTo(CHECKSUM);
    assertThat(doc.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
  }

  @Test
  @DisplayName("Should transition to STORED and assign storageKey when markStored is called on PDF_GENERATED document")
  void should_transitionToStored_when_markStoredCalledOnPdfGeneratedDocument() {
    var doc = buildPdfGeneratedDocument();
    var updatedAtBefore = doc.getUpdatedAt();

    doc.markStored(STORAGE_KEY);

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.STORED);
    assertThat(doc.getStorageKey()).isEqualTo(STORAGE_KEY);
    assertThat(doc.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
  }

  @Test
  @DisplayName("Should transition to PUBLISHED and set publishedAt equal to updatedAt when markPublished is called on STORED document")
  void should_transitionToPublished_when_markPublishedCalledOnStoredDocument() {
    var doc = buildStoredDocument();

    doc.markPublished();

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PUBLISHED);
    assertThat(doc.getPublishedAt()).isNotNull();
    assertThat(doc.getUpdatedAt()).isEqualTo(doc.getPublishedAt());
  }

  @Test
  @DisplayName("Should complete full lifecycle from PENDING to PUBLISHED")
  void should_completeFullLifecycle_when_allTransitionsAppliedInOrder() {
    var doc = DigitalDocument.createPending(EMPLOYEE_ID, MANAGED_GROUP_ID);
    var employeeData = EmployeeData.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .build();

    doc.markEnriched(employeeData);
    doc.markPdfGenerated(CHECKSUM);
    doc.markStored(STORAGE_KEY);
    doc.markPublished();

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PUBLISHED);
    assertThat(doc.getEmployeeData()).isEqualTo(employeeData);
    assertThat(doc.getChecksum()).isEqualTo(CHECKSUM);
    assertThat(doc.getStorageKey()).isEqualTo(STORAGE_KEY);
    assertThat(doc.getPublishedAt()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // 3. Invalid transitions — markEnriched
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> markEnriched from {0} should throw")
  @EnumSource(value = DocumentStatus.class, names = {"ENRICHED", "PDF_GENERATED", "STORED", "PUBLISHED", "FAILED"})
  @DisplayName("Should throw DigitalDocumentStateException when markEnriched is called from an invalid status")
  void should_throwStateException_when_markEnrichedCalledFromInvalidStatus(DocumentStatus status) {
    var doc = buildDocumentWithStatus(status);
    var employeeData = EmployeeData.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .build();

    assertThatThrownBy(() -> doc.markEnriched(employeeData))
        .isInstanceOf(DigitalDocumentStateException.class)
        .hasMessageContaining("ENRICHED");
  }

  // ---------------------------------------------------------------------------
  // 3. Invalid transitions — markPdfGenerated
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> markPdfGenerated from {0} should throw")
  @EnumSource(value = DocumentStatus.class, names = {"PENDING", "PDF_GENERATED", "STORED", "PUBLISHED", "FAILED"})
  @DisplayName("Should throw DigitalDocumentStateException when markPdfGenerated is called from an invalid status")
  void should_throwStateException_when_markPdfGeneratedCalledFromInvalidStatus(DocumentStatus status) {
    var doc = buildDocumentWithStatus(status);

    assertThatThrownBy(() -> doc.markPdfGenerated(CHECKSUM))
        .isInstanceOf(DigitalDocumentStateException.class)
        .hasMessageContaining("PDF_GENERATED");
  }

  // ---------------------------------------------------------------------------
  // 3. Invalid transitions — markStored
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> markStored from {0} should throw")
  @EnumSource(value = DocumentStatus.class, names = {"PENDING", "ENRICHED", "STORED", "PUBLISHED", "FAILED"})
  @DisplayName("Should throw DigitalDocumentStateException when markStored is called from an invalid status")
  void should_throwStateException_when_markStoredCalledFromInvalidStatus(DocumentStatus status) {
    var doc = buildDocumentWithStatus(status);

    assertThatThrownBy(() -> doc.markStored(STORAGE_KEY))
        .isInstanceOf(DigitalDocumentStateException.class)
        .hasMessageContaining("STORED");
  }

  // ---------------------------------------------------------------------------
  // 3. Invalid transitions — markPublished
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> markPublished from {0} should throw")
  @EnumSource(value = DocumentStatus.class, names = {"PENDING", "ENRICHED", "PDF_GENERATED", "PUBLISHED", "FAILED"})
  @DisplayName("Should throw DigitalDocumentStateException when markPublished is called from an invalid status")
  void should_throwStateException_when_markPublishedCalledFromInvalidStatus(DocumentStatus status) {
    var doc = buildDocumentWithStatus(status);

    assertThatThrownBy(() -> doc.markPublished())
        .isInstanceOf(DigitalDocumentStateException.class)
        .hasMessageContaining("PUBLISHED");
  }

  // ---------------------------------------------------------------------------
  // 4. markFailed — allowed statuses
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> markFailed from {0} should succeed")
  @EnumSource(value = DocumentStatus.class, names = {"PENDING", "ENRICHED", "PDF_GENERATED", "STORED"})
  @DisplayName("Should transition to FAILED and assign error details when markFailed is called from a non-PUBLISHED status")
  void should_transitionToFailed_when_markFailedCalledFromNonPublishedStatus(DocumentStatus status) {
    var doc = buildDocumentWithStatus(status);

    doc.markFailed(FailedStep.ENRICHMENT, ERROR_CODE, ERROR_MESSAGE);

    assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
    assertThat(doc.getFailedStep()).isEqualTo(FailedStep.ENRICHMENT);
    assertThat(doc.getLastErrorCode()).isEqualTo(ERROR_CODE);
    assertThat(doc.getLastErrorMessage()).isEqualTo(ERROR_MESSAGE);
    assertThat(doc.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should throw DigitalDocumentStateException when markFailed is called on a PUBLISHED document")
  void should_throwStateException_when_markFailedCalledOnPublishedDocument() {
    var doc = buildDocumentWithStatus(DocumentStatus.PUBLISHED);

    assertThatThrownBy(() -> doc.markFailed(FailedStep.ENRICHMENT, ERROR_CODE, ERROR_MESSAGE))
        .isInstanceOf(DigitalDocumentStateException.class)
        .hasMessageContaining("PUBLISHED");
  }

  // ---------------------------------------------------------------------------
  // 5. prepareForRetry — recovery status per FailedStep
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> FAILED with failedStep={0} should recover to {1}")
  @MethodSource("failedStepToRecoveryStatus")
  @DisplayName("Should recover to the correct status for each FailedStep when prepareForRetry is called")
  void should_recoverToCorrectStatus_when_prepareForRetryCalledWithFailedStep(
      FailedStep failedStep, DocumentStatus expectedStatus) {
    var doc = buildFailedDocument(failedStep);

    doc.prepareForRetry();

    assertThat(doc.getStatus()).isEqualTo(expectedStatus);
  }

  static Stream<Arguments> failedStepToRecoveryStatus() {
    return Stream.of(
        Arguments.of(FailedStep.ENRICHMENT,    DocumentStatus.PENDING),
        Arguments.of(FailedStep.PDF_GENERATION, DocumentStatus.ENRICHED),
        Arguments.of(FailedStep.STORAGE,        DocumentStatus.PDF_GENERATED),
        Arguments.of(FailedStep.PUBLICATION,    DocumentStatus.STORED)
    );
  }

  @Test
  @DisplayName("Should increment retryCount to 1 and clear error fields when prepareForRetry is called from FAILED with ENRICHMENT")
  void should_incrementRetryCountAndClearErrors_when_prepareForRetryCalledFromFailed() {
    var doc = buildFailedDocument(FailedStep.ENRICHMENT);

    doc.prepareForRetry();

    assertThat(doc.getRetryCount()).isEqualTo(1);
    assertThat(doc.getFailedStep()).isNull();
    assertThat(doc.getLastErrorCode()).isNull();
    assertThat(doc.getLastErrorMessage()).isNull();
  }

  @Test
  @DisplayName("Should set nextRetryAt strictly after now when prepareForRetry is called")
  void should_setNextRetryAtAfterNow_when_prepareForRetryIsCalled() {
    var doc = buildFailedDocument(FailedStep.ENRICHMENT);
    var before = OffsetDateTime.now();

    doc.prepareForRetry();

    assertThat(doc.getNextRetryAt()).isAfter(before);
  }

  @Test
  @DisplayName("Should set nextRetryAt approximately now+60s when retryCount=0 and prepareForRetry is called")
  void should_setNextRetryAtNowPlus60s_when_retryCountIsZero() {
    var doc = buildFailedDocument(FailedStep.ENRICHMENT); // retryCount=0
    var before = OffsetDateTime.now();

    doc.prepareForRetry();

    var expectedNextRetry = before.plusSeconds(60);
    assertThat(doc.getNextRetryAt())
        .isCloseTo(expectedNextRetry, within(2, ChronoUnit.SECONDS));
  }

  @Test
  @DisplayName("Should set nextRetryAt approximately now+240s when retryCount=2 and prepareForRetry is called")
  void should_setNextRetryAtNowPlus240s_when_retryCountIsTwo() {
    var doc = DigitalDocument.builder()
        .employeeId(EMPLOYEE_ID)
        .managedGroupId(MANAGED_GROUP_ID)
        .status(DocumentStatus.FAILED)
        .failedStep(FailedStep.ENRICHMENT)
        .lastErrorCode(ERROR_CODE)
        .lastErrorMessage(ERROR_MESSAGE)
        .retryCount(2)
        .version(0L)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
    var before = OffsetDateTime.now();

    doc.prepareForRetry();

    var expectedNextRetry = before.plusSeconds(240);
    assertThat(doc.getNextRetryAt())
        .isCloseTo(expectedNextRetry, within(2, ChronoUnit.SECONDS));
  }

  @ParameterizedTest(name = "{index} -> prepareForRetry from {0} (non-FAILED) should throw")
  @EnumSource(value = DocumentStatus.class, names = {"PENDING", "ENRICHED", "PDF_GENERATED", "STORED", "PUBLISHED"})
  @DisplayName("Should throw DigitalDocumentStateException when prepareForRetry is called from a non-FAILED status")
  void should_throwStateException_when_prepareForRetryCalledFromNonFailedStatus(DocumentStatus status) {
    var doc = buildDocumentWithStatus(status);

    assertThatThrownBy(() -> doc.prepareForRetry())
        .isInstanceOf(DigitalDocumentStateException.class)
        .hasMessageContaining("FAILED");
  }

  // ---------------------------------------------------------------------------
  // 6. FailedStep.getRecoveryStatus — parametrizado
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{index} -> FailedStep.{0}.getRecoveryStatus() == {1}")
  @MethodSource("failedStepRecoveryStatusMappings")
  @DisplayName("Should return the correct recovery DocumentStatus for each FailedStep")
  void should_returnCorrectRecoveryStatus_when_getRecoveryStatusCalled(
      FailedStep failedStep, DocumentStatus expectedRecoveryStatus) {

    assertThat(failedStep.getRecoveryStatus()).isEqualTo(expectedRecoveryStatus);
  }

  static Stream<Arguments> failedStepRecoveryStatusMappings() {
    return Stream.of(
        Arguments.of(FailedStep.ENRICHMENT,    DocumentStatus.PENDING),
        Arguments.of(FailedStep.PDF_GENERATION, DocumentStatus.ENRICHED),
        Arguments.of(FailedStep.STORAGE,        DocumentStatus.PDF_GENERATED),
        Arguments.of(FailedStep.PUBLICATION,    DocumentStatus.STORED)
    );
  }
}
