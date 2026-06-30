package com.mercadona.prueba.snk.digitaldocument.application.ports.driving;

import com.mercadona.prueba.snk.digitaldocument.application.usecases.ReceiveEmployeeEventResult;

public interface ReceiveEmployeeEventPort {

  ReceiveEmployeeEventResult receive(String employeeId, String managedGroupId);
}
