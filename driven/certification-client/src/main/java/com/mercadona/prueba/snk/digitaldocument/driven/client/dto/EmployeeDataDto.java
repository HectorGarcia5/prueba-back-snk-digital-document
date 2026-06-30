package com.mercadona.prueba.snk.digitaldocument.driven.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmployeeDataDto {

  private String employeeId;
  private String managedGroupId;
  private String fullName;
  private String jobFunction;
  private String department;
  private String email;
  private String phoneExtension;
  private String location;
  private AiCertificationDataDto certification;
}
