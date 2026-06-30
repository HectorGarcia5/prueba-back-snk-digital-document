# Estado de implementación

## Progreso por fases

- [x] **Fase 1**: Esqueleto de los 3 proyectos + vinculación a repos GitHub
- [x] **Fase 2**: Dominio y máquina de estados (`DigitalDocument`, `DocumentStatus`, `FailedStep`, `EmployeeData`) — 52 tests ✓
- [ ] **Fase 3**: Persistencia PostgreSQL y Flyway
- [ ] **Fase 4**: Recepción idempotente del evento (`ReceiveEmployeeEventUseCase`)
- [ ] **Fase 5**: Cliente de enriquecimiento (API externa cardgenerator)
- [ ] **Fase 6**: Generación del PDF (`PdfGenerator`)
- [ ] **Fase 7**: Almacenamiento S3/MinIO (`DocumentStorage`)
- [ ] **Fase 8**: Orquestación del flujo (`ProcessDigitalDocumentUseCase`)
- [ ] **Fase 9**: Transactional Outbox
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
| Outbox para consistencia STORED↔PUBLISHED | Evita publicar en Kafka si el PDF no está almacenado correctamente |
| `FailedStep` contiene `getRecoveryStatus()` | Evita switch/if en `prepareForRetry()`; el enum lleva la lógica de recuperación |
| `EmployeeData` como placeholder en Fase 2 | Contrato real de cardgenerator se inspecciona en Fase 5 (Swagger en localhost:8081) |
| Dominio sin anotaciones JPA/Spring | Pureza del dominio; entidades JPA separadas en `driven/postgres-repository` |
| Backoff exponencial: 60s * 2^retryCount, cap 1h | Balance entre reintento rápido y no saturar servicios externos |

---

## Pendientes / Limitaciones conocidas

- `EmployeeData`: los campos concretos se definirán en Fase 5 tras inspeccionar el Swagger de cardgenerator
- Esquema Avro de `employee-digital-document` (campos `employeeId`, `managedGroupId`, `digitalDocumentId`): resto de campos con valor por defecto según README de apoyo
- Configuración SSL/SASL de Kafka en `application-dev.yml`: pendiente de credenciales de entorno
- `POST /api/v1/utils/documents/publish` del WEB: diseñar en Fase 11 junto al BTC
