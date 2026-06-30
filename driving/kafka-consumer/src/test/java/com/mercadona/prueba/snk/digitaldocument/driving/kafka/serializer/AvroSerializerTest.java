package com.mercadona.prueba.snk.digitaldocument.driving.kafka.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thirdparty.employee.employee.EmployeeEventPublicKey;
import thirdparty.employee.employee.ManagedGroupIds;

class AvroSerializerTest {

  private final AvroSerializer<SpecificRecordBase> serializer = new AvroSerializer<>();

  // ---------------------------------------------------------------------------
  // serialize — happy path
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return non-blank JSON string when serializing a valid Avro record")
  void should_return_non_blank_json_when_serializing_valid_record() {
    EmployeeEventPublicKey key = buildKey("12345", "GRP-01");

    String result = serializer.serialize(key);

    assertThat(result).isNotBlank();
    assertThat(result).contains("12345");
  }

  @Test
  @DisplayName("Should return valid JSON object when serializing an Avro record")
  void should_return_valid_json_object_for_avro_record() {
    EmployeeEventPublicKey key = buildKey("99", "GRP-02");

    String result = serializer.serialize(key);

    assertThat(result).startsWith("{");
    assertThat(result).endsWith("}");
  }

  // ---------------------------------------------------------------------------
  // serialize — error path (IOException from bad schema/writer)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should throw IllegalStateException when Avro record getSchema throws RuntimeException")
  void should_throw_illegal_state_exception_when_get_schema_throws() {
    SpecificRecordBase brokenRecord = mock(SpecificRecordBase.class);
    when(brokenRecord.getSchema()).thenThrow(new RuntimeException("bad schema"));

    assertThatThrownBy(() -> serializer.serialize(brokenRecord))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Error serializing Avro record");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static EmployeeEventPublicKey buildKey(String id, String managedGroupId) {
    ManagedGroupIds mgIds = ManagedGroupIds.newBuilder().setId(managedGroupId).build();
    return EmployeeEventPublicKey.newBuilder()
        .setId(id)
        .setManagedGroupId(mgIds)
        .build();
  }
}
