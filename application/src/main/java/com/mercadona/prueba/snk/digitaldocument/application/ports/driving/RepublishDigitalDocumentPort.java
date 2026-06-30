package com.mercadona.prueba.snk.digitaldocument.application.ports.driving;

import java.util.UUID;

public interface RepublishDigitalDocumentPort {

  void republish(UUID documentId, String employeeId, String managedGroupId);
}
