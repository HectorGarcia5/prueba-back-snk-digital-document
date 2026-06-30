package com.mercadona.prueba.snk.digitaldocument.application.usecases;

import java.util.UUID;

/**
 * Result of receiving an employee event: the classification and the document ID
 * (new or existing) so the caller can trigger further processing if needed.
 */
public record ReceiveEmployeeEventResponse(ReceiveEmployeeEventResult result, UUID documentId) {
}
