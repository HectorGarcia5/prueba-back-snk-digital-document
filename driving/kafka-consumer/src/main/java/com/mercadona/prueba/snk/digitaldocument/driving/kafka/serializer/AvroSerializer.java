package com.mercadona.prueba.snk.digitaldocument.driving.kafka.serializer;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer<T extends SpecificRecordBase> {

  public String serialize(final T data) {
    try (var out = new ByteArrayOutputStream()) {
      DatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
      var encoder = EncoderFactory.get().jsonEncoder(data.getSchema(), out);
      writer.write(data, encoder);
      encoder.flush();
      return out.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Error serializing Avro record: " + ex.getMessage(), ex);
    }
  }
}
