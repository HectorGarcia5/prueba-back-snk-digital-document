package com.mercadona.prueba.snk.digitaldocument.driven.repositories.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadona.prueba.snk.digitaldocument.domain.AiCertificationData;
import com.mercadona.prueba.snk.digitaldocument.domain.DigitalDocument;
import com.mercadona.prueba.snk.digitaldocument.domain.EmployeeData;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class DigitalDocumentMapper {

  @Autowired
  protected ObjectMapper objectMapper;

  @Mapping(target = "employeeData", expression = "java(toJson(domain.getEmployeeData()))")
  public abstract DigitalDocumentMO toMO(DigitalDocument domain);

  @Mapping(target = "employeeData", expression = "java(fromJson(mo.getEmployeeData()))")
  public abstract DigitalDocument toDomain(DigitalDocumentMO mo);

  protected String toJson(EmployeeData data) {
    if (data == null) return null;
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot serialize EmployeeData", e);
    }
  }

  protected EmployeeData fromJson(String json) {
    if (json == null) return null;
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode cert = root.path("certification");
      return EmployeeData.builder()
          .employeeId(text(root, "employeeId"))
          .managedGroupId(text(root, "managedGroupId"))
          .fullName(text(root, "fullName"))
          .jobFunction(text(root, "jobFunction"))
          .department(text(root, "department"))
          .email(text(root, "email"))
          .phoneExtension(text(root, "phoneExtension"))
          .location(text(root, "location"))
          .certification(cert.isMissingNode() ? null : AiCertificationData.builder()
              .certificationId(text(cert, "certificationId"))
              .status(text(cert, "status"))
              .valid(cert.path("valid").asBoolean(false))
              .startDate(parseDate(cert, "startDate"))
              .expirationDate(parseDate(cert, "expirationDate"))
              .issuedDate(parseDate(cert, "issuedDate"))
              .level(text(cert, "level"))
              .approvedTools(parseList(cert.path("approvedTools")))
              .issuedBy(text(cert, "issuedBy"))
              .description(text(cert, "description"))
              .build())
          .build();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot deserialize EmployeeData", e);
    }
  }

  private String text(JsonNode node, String field) {
    JsonNode n = node.path(field);
    return n.isNull() || n.isMissingNode() ? null : n.asText();
  }

  private LocalDate parseDate(JsonNode node, String field) {
    JsonNode n = node.path(field);
    if (n.isNull() || n.isMissingNode()) return null;
    return LocalDate.parse(n.asText());
  }

  private List<String> parseList(JsonNode node) {
    if (node.isNull() || node.isMissingNode()) return List.of();
    List<String> list = new ArrayList<>();
    node.forEach(n -> list.add(n.asText()));
    return list;
  }
}
