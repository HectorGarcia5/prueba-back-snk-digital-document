package com.mercadona.prueba.snk.digitaldocument.application.ports.driving;

import com.mercadona.prueba.snk.digitaldocument.application.usecases.ReceiveEmployeeEventResponse;

public interface ReceiveEmployeeEventPort {

  ReceiveEmployeeEventResponse receive(String employeeId, String managedGroupId);
}
