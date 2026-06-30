package com.mercadona.prueba.snk.digitaldocument.application.ports.driven;

import com.mercadona.prueba.snk.digitaldocument.application.model.PdfGenerationRequest;
import com.mercadona.prueba.snk.digitaldocument.application.model.PdfResult;

public interface PdfGenerator {

  /**
   * Generates a PDF certificate for the given employee.
   * Generation is deterministic: the same request always produces the same bytes.
   */
  PdfResult generate(PdfGenerationRequest request);
}
