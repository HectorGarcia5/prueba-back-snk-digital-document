# Estado de implementación

## Progreso por fases

- [x] **Fase 1**: Esqueleto de los 3 proyectos + vinculación a repos GitHub
- [x] **Fase 2**: Dominio y máquina de estados (`DigitalDocument`, `DocumentStatus`, `FailedStep`, `EmployeeData`) — 52 tests ✓
- [x] **Fase 3**: Persistencia PostgreSQL y Flyway — 16 tests (Testcontainers) ✓
- [x] **Fase 4**: Recepción idempotente del evento (`ReceiveEmployeeEventUseCase`) ✓
- [x] **Fase 5**: Cliente de enriquecimiento — contrato real cardgenerator + Resilience4j ✓
- [ ] **Fase 6**: Generación del PDF (`PdfGenerator`)
- [ ] **Fase 7**: Almacenamiento MinIO (`DocumentStorage` vía `fwkcna-starter-buckets`)
- [ ] **Fase 8**: Transactional Outbox ← **adelantado** (ver decisión abajo)
- [ ] **Fase 9**: Orquestación completa (`ProcessDigitalDocumentUseCase`) — STORED + Outbox en una tx
- [ ] **Fase 10**: Kafka consumer y publisher
- [ ] **Fase 11**: Batch de recuperación (BTC micro)
- [ ] **Fase 12**: API REST de consulta (WEB micro)
- [ ] **Fase 13**: Observabilidad
- [ ] **Fase 14**: Docker Compose y documentación final

---

## Decisiones tomadas

| Decisión | Razón |
|---|---|
| 3 micros (SNK + WEB + BTC) | Separación de responsabilidades alineada con patrones CNA Mercadona |
| Batch en micro propio (BTC) | Escalado independiente; sigue el patrón `labmng-back-btc-recovery` |
| BTC llama al WEB para publicar | WEB es el Kafka producer de `employee-digital-document`; BTC solo orquesta |
| Flyway solo en SNK | SNK es dueño del esquema; WEB y BTC acceden en lectura |
| Outbox **antes** de la orquestación completa | STORED + creación OutboxEvent deben ser una sola tx; implementar Outbox primero evita inconsistencia |
| `FailedStep` contiene `getRecoveryStatus()` | Evita switch/if en `prepareForRetry()`; PUBLICATION → devuelve STORED (reintenta Outbox, no reprocesa PDF) |
| PDF regenerable determinísticamente | Si cae entre PDF_GENERATED y STORED, regenerar desde `employeeData` persistido + `createdAt`; no se persiste el binario |
| `digitalDocumentId` en evento Avro | El campo interno del dominio es `id` (UUID); el campo Avro de salida es `digitalDocumentId` |
| `publicationReason` en OutboxEvent | INITIAL / DUPLICATE_EVENT / MANUAL_RETRY para no bloquear republicaciones legítimas |
| Kafka at-least-once (deliberado) | Se garantiza: unicidad del documento, nunca publicar antes de almacenar, uno o más envíos del mismo `digitalDocumentId`. El consumidor de `employee-digital-document` debe ser idempotente usando `digitalDocumentId` como clave lógica |
| Dominio sin anotaciones JPA/Spring | Pureza del dominio; entidades JPA separadas en `driven/postgres-repository` |
| Backoff exponencial: 60s * 2^retryCount, cap 1h | Balance entre reintento rápido y no saturar servicios externos |

---

## Limitaciones conocidas

- Configuración SSL/SASL de Kafka en `application-dev.yml`: pendiente de credenciales de entorno real
- Dependencias Avro (`employee:1.2.1`, `employeedigitaldocumentv0:1.1.0`) requieren repositorio Maven corporativo y Schema Registry; si no son resolubles públicamente, los adaptadores Kafka quedan aislados detrás de sus puertos
- `POST /api/v1/utils/documents/publish` del WEB: diseñar en Fase 11 junto al BTC
