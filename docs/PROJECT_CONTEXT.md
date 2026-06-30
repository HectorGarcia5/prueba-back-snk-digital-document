# Contexto del proyecto

## Objetivo funcional

Sistema de generación automática de documentos digitales de certificación para empleados de Mercadona.
Cuando se produce un evento relacionado con un empleado, el sistema genera un PDF, lo almacena y notifica
su disponibilidad a otros sistemas.

---

## Arquitectura — 3 microservicios

| Micro | Repo | Responsabilidad |
|---|---|---|
| `prueba-back-snk-digital-document` | HectorGarcia5/prueba-back-snk-digital-document | Kafka consumer + procesado completo + Outbox publisher |
| `prueba-back-web-digital-document` | HectorGarcia5/prueba-back-web-digital-document | REST API de consulta + endpoint de publicación para BTC |
| `prueba-back-btc-digital-document` | HectorGarcia5/prueba-back-btc-digital-document | Batch recovery: lee FAILED → llama WEB → publica evento |

Base de datos PostgreSQL compartida. Flyway y propiedad del esquema en el SNK.

---

## Flujo principal

```
[Kafka: employee]
        │
        ▼
[SNK] Recibe evento (employeeId + managedGroupId)
        │
        ▼ PENDING
[SNK] Enriquece — llama API externa cardgenerator (puerto 8081)
        │
        ▼ ENRICHED
[SNK] Genera PDF con documentId único
        │
        ▼ PDF_GENERATED
[SNK] Sube PDF a MinIO/S3
        │
        ▼ STORED  ──── (en la misma tx) ────► OutboxEvent creado en BD
        │
        ▼ PUBLISHED
[SNK] Outbox publisher publica → [Kafka: employee-digital-document]
```

Si falla cualquier paso:
```
estado actual → FAILED  (failedStep indica el paso concreto)
```

---

## Ciclo de vida del documento

| Estado | Descripción |
|---|---|
| `PENDING` | Evento recibido, registro creado |
| `ENRICHED` | Datos del empleado obtenidos de la API externa |
| `PDF_GENERATED` | PDF generado con éxito |
| `STORED` | PDF subido al bucket de almacenamiento |
| `PUBLISHED` | Evento publicado en Kafka |
| `FAILED` | Fallo en algún paso — `failedStep` indica cuál |

Transición de estados:
```
PENDING → ENRICHED → PDF_GENERATED → STORED → PUBLISHED
Cualquier estado procesable → FAILED
```

Pasos susceptibles de fallo (`failedStep`):
```
ENRICHMENT | PDF_GENERATION | STORAGE | PUBLICATION
```

---

## Idempotencia

Un único documento por `(employeeId, managedGroupId)`.
Restricción UNIQUE en base de datos. La garantía final es el constraint de PostgreSQL, no un find+save.

Si llega un evento duplicado:
- Documento en STORED o PUBLISHED → republicar `documentId` a `employee-digital-document`
- Documento en proceso → no iniciar otro procesamiento
- Documento en FAILED → disponible para reprocesamiento por el BTC

---

## Consistencia — Transactional Outbox

La transición a `STORED` y la creación del `OutboxEvent` se realizan en la misma transacción de BD.
No se publica el evento Kafka si el PDF no está correctamente almacenado.

---

## Batch de recuperación (BTC)

1. Lee `digital_document` con `status = FAILED` y `failed_step = 'PUBLICATION'` usando `FOR UPDATE SKIP LOCKED`
2. Llama `POST /api/v1/utils/documents/publish` en el WEB micro
3. El WEB publica a `employee-digital-document`
4. BTC marca el documento como procesado

---

## API REST (WEB micro)

| Endpoint | Descripción |
|---|---|
| `GET /api/v1/employees/{employeeId}/managed-groups/{managedGroupId}/document` | Documento de un empleado |
| `GET /api/v1/documents/{documentId}` | Consulta por ID |
| `GET /api/v1/documents/{documentId}/status` | Estado actual |
| `GET /api/v1/documents?status={status}&page={page}&size={size}` | Listado por estado |
| `POST /api/v1/utils/documents/publish` | Endpoint interno para el BTC |

---

## Kafka

| Tópico | Dirección | Avro | Versión |
|---|---|---|---|
| `employee` | Consumer (entrada) | `com.mercadona.kafka.schemas.thirdparty.employee:employee` | 1.2.1 |
| `employee-digital-document` | Producer (salida) | `...employeedigitaldocumentv0` | 1.1.0 |

Campos del evento de salida: `employeeId`, `managedGroupId`, `digitalDocumentId`.

---

## API externa de enriquecimiento

- Imagen Docker: `oromerji/prueba-rrhh-cardgenerator`
- Puerto: `8081`
- Swagger: `http://localhost:8081/swagger-ui/index.html`
- **No inventar el contrato** — inspeccionar Swagger en Fase 5 antes de implementar

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 + Spring Boot 3 | Runtime |
| fwkcna-parent 5.2.1 | Framework CNA Mercadona |
| PostgreSQL + Flyway | Persistencia + migraciones |
| Spring Data JPA | ORM |
| Apache Kafka + Avro | Eventos |
| MinIO (S3-compatible) | Almacenamiento de PDFs |
| Resilience4j | Resiliencia en cliente REST externo |
| Testcontainers | Tests de integración |
| WireMock | Tests del cliente REST externo |
| Micrometer + Actuator | Observabilidad |

---

## Reglas de implementación

- No implementar todo de una vez — trabajar fase a fase
- El dominio no debe depender de Spring, JPA, Kafka, S3 ni librerías de infraestructura
- No cambiar directamente el estado con setters — usar métodos de dominio
- No mantener una transacción de BD abierta mientras se llama a servicios externos
- Transacciones cortas
- No duplicar clases ni responsabilidades
- Reutilizar el código existente
- Toda nueva funcionalidad incluye pruebas
