# prueba-back-snk-digital-document

Microservicio SNK de generación de documentos digitales de empleados.
Es el **núcleo del sistema**: consume eventos Kafka, enriquece datos, genera PDF, almacena en MinIO y publica el evento de resultado.

---

## Arquitectura del sistema

El sistema está compuesto por tres microservicios independientes:

```
  Kafka (employee topic)
        │
        ▼
┌──────────────────┐      Certification API      ┌──────────────────┐
│  SNK (este micro)│ ──────────────────────────▶ │  cardgenerator   │
│  :8080           │                              │  :8081           │
│                  │      MinIO (S3)              └──────────────────┘
│  - Kafka consumer│ ──────────────────────────▶ employee-documents/
│  - PDF generator │                              {documentId}.pdf
│  - Outbox relay  │ ──────────────────────────▶ Kafka (employeedigitaldocument)
└────────┬─────────┘
         │ PostgreSQL (digital_document schema)
         ▼
┌──────────────────┐      ┌──────────────────┐
│  WEB  :8083      │      │  BTC  (batch)    │
│  REST query API  │      │  Recovery batch  │
│  5 endpoints     │      │  FWKBatch3       │
└──────────────────┘      └──────────────────┘
```

> Los tres micros comparten la misma base de datos PostgreSQL.
> La documentación completa de arquitectura está en este README.
> **WEB**: ver [prueba-back-web-digital-document](../prueba-back-web-digital-document/README.md)
> **BTC**: ver [prueba-back-btc-digital-document](../prueba-back-btc-digital-document/README.md)

---

## Flujo principal

```
Kafka (employee event)
    │
    ├─▶ ReceiveEmployeeEventUseCase
    │       └─ Crea documento PENDING (idempotente: unique constraint DB)
    │
    └─▶ ProcessDigitalDocumentUseCase (pipeline encadenado)
            │
            ├─ PENDING  ──▶ CertificationApiClientAdapter ──▶ ENRICHED
            ├─ ENRICHED ──▶ PdfGeneratorAdapter            ──▶ PDF_GENERATED
            ├─ PDF_GENERATED ─▶ DocumentStorageAdapter (MinIO) ──▶ STORED
            └─ STORED   ──▶ OutboxEventAdapter (o_outbox) + markPublished ──▶ PUBLISHED
                                    │
                                    └─▶ Outbox relay ──▶ Kafka (employeedigitaldocument)
```

---

## Modelo de estados del documento

```
PENDING ──▶ ENRICHED ──▶ PDF_GENERATED ──▶ STORED ──▶ PUBLISHED
   │            │               │              │
   └────────────┴───────────────┴──────────────┴──▶ FAILED (failedStep indica el paso)
```

| Estado        | Descripción                                         |
|---------------|-----------------------------------------------------|
| PENDING       | Evento recibido, documento creado                   |
| ENRICHED      | Datos del empleado obtenidos de la API externa      |
| PDF_GENERATED | PDF generado (SHA-256 calculado)                    |
| STORED        | PDF subido a MinIO                                  |
| PUBLISHED     | Evento Kafka publicado                              |
| FAILED        | Fallo en algún paso (`failedStep` indica cuál)      |

---

## Tópicos Kafka

| Dirección | Tópico                                                                           | Schema Avro              |
|-----------|----------------------------------------------------------------------------------|--------------------------|
| Consumer  | `thirdparty.employee.employee.event.public.v0.table.cpd`                         | `employee:1.2.1`         |
| Producer  | `thirdparty.employee.employeedigitaldocument.event.restrictedout.v0.table.cpd`   | `employeedigitaldocumentv0:1.1.0` |

---

## Estrategia de idempotencia

- Restricción única en BD: `(employee_id, managed_group_id)` — nunca se crean dos documentos para el mismo empleado.
- Si llega un evento duplicado:
  - Documento ya `STORED` o `PUBLISHED` → republicación via Outbox sin crear documento nuevo.
  - Documento en proceso → se ignora (`DUPLICATE_IN_PROGRESS`).
  - Documento en `FAILED` → disponible para reintento por el batch BTC.

---

## Transactional Outbox

- Al llegar a `STORED`, **en la misma transacción** se escribe en `o_outbox` y se marca `PUBLISHED`.
- Un relay externo (fwkcna-outbox) lee la tabla y publica en Kafka. Garantía: at-least-once.
- Si el relay falla, el documento queda `STORED` con el evento en `o_outbox` pendiente hasta el próximo intento.

---

## Gestión de errores y reintentos

### Errores de negocio (procesamiento del documento)

- Cualquier fallo en el pipeline persiste `status=FAILED`, `failed_step` y `last_error_message`.
- Backoff exponencial: `next_retry_at = NOW() + 1min * 2^retry_count` (máx 1h).
- El **BTC** (`prueba-back-btc-digital-document`) procesa automáticamente los documentos `FAILED` con `failed_step=PUBLICATION`.
- El endpoint `POST /api/v1/documents/{id}/retry` (en el WEB) permite reinicio manual del backoff.

### Errores del consumer Kafka (`topic_consumer_error`)

Cuando el consumer recibe un mensaje y falla con una excepción no controlada (ajena al procesamiento normal del documento), el error se persiste en la tabla `topic_consumer_error` y el offset se **acknowledges** — el mensaje no se reintenta, se descarta y el error queda trazado.

| Campo           | Descripción                                      |
|-----------------|--------------------------------------------------|
| `tce_topic_name`| Nombre del topic Kafka donde llegó el mensaje    |
| `event_key`     | Key Avro serializada como JSON                   |
| `event_payload` | Value Avro serializado como JSON                 |
| `event_offset`  | Offset del mensaje                               |
| `error`         | Causa raíz de la excepción                       |
| `creation_date` | Timestamp del error                              |

La métrica `prueba_snk_error_topicconsume_total` (Micrometer `Gauge`) expone el número de filas de esta tabla en `/actuator/prometheus`, permitiendo alertar si el contador crece.

---

## Stack técnico

- Java 21, Spring Boot 3.3.x, fwkcna-parent 5.2.1
- PostgreSQL 16, Flyway, Spring Data JPA + HikariCP
- Apache Kafka + Confluent Avro (fwkcna-starter-kafka-consumer)
- MinIO S3 (fwkcna-starter-buckets)
- OpenPDF (generación de PDF)
- Transactional Outbox (fwkcna-starter-outbox-avro-jpa-register)
- Resilience4j (reintentos en CertificationApiClientAdapter)

---

## Instrucciones de arranque local

### 1. Requisitos previos

```bash
# Stack de infraestructura (desde prueba-back-snk-digital-document/devops/docker/)
docker compose up -d postgres minio kafka schema-registry cardgenerator
docker compose up -d minio-init kafka-init
```

### 2. Migraciones Flyway

```bash
mvn -pl driven/postgres-repository flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/digital_document \
  -Dflyway.schemas=digital_document \
  -Dflyway.user=sa \
  -Dflyway.password=root \
  -Dflyway.locations=filesystem:driven/postgres-repository/sql/migration/versions
```

### 3. Arrancar el micro

```bash
mvn --projects boot spring-boot:run -Dspring-boot.run.profiles=local
```

Puerto: **8080**

---

## Migraciones de base de datos

| Versión | Descripción                              |
|---------|------------------------------------------|
| V1.0.0  | Tabla `digital_document` + índices                          |
| V1.1.0  | Columna `employee_data` (JSON)                              |
| V1.2.0  | Tabla `o_outbox` + secuencia (Transactional Outbox)          |
| V1.3.0  | Tabla `digital_document_recovery_tmp` (batch BTC)            |
| V1.4.0  | Tabla `topic_consumer_error` + índices (errores Kafka)       |

---

## Ejecución de tests

```bash
# Tests unitarios (sin Docker)
mvn test -pl application -pl driving/kafka-consumer

# Tests de integración del mapper (sin Docker)
mvn test -pl driven/postgres-repository

# Test completo (requiere Docker para Testcontainers)
mvn test
```

---

## Limitaciones conocidas

- El consumer Kafka requiere perfil distinto de `local` para funcionar (`fwkcna.kafka.consumer.enabled: false` en local).
- Los schemas Avro (`employee:1.2.1`, `employeedigitaldocumentv0:1.1.0`) deben estar registrados en el Schema Registry antes del primer mensaje.
- El PDF no es determinista entre ejecuciones (OpenPDF genera un FileID diferente en cada generación); la comparación de checksums entre pasos se ha desactivado.
- La configuración de SASL/SSL para Kafka en entornos `dev`/`itg` requiere credenciales corporativas.

