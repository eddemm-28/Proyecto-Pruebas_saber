# A01 — Servicios, límites, endpoints REST y eventos RabbitMQ

> **Corte 2 · Sprint A · Diseño Ronda 1** · Responsable: Integrante 3 · Talla S
> Proyecto: *Sistema para la Gestión, Validación y Administración de un Banco de Preguntas para la Preparación de las Pruebas Saber Pro*

## 1. Alcance del Corte 2

| Incluido en Corte 2 | Excluido (Corte 3) |
|---|---|
| HU de usuarios (RF-01 a RF-03), JWT y roles (RNF-06, RNF-07), auditoría de logins (RNF-09) | **HU05 completa – Revisión por pares** (RF-16 a RF-20): asignar revisores, formato de evaluación, observaciones, historial de revisiones |
| Banco de preguntas (RF-05 a RF-15), State de 7 estados, validadores, Microkernel/Plugins, historial (RF-29, RF-30) | Paneles avanzados de auditoría global de **HU08** |
| **A19:** endpoints mínimos para aprobar y publicar (sustituyen temporalmente HU05) | Integraciones externas reales |
| HU06 Simulacros (RF-21 a RF-24) y HU07 Seguimiento (RF-25 a RF-28) en hexagonal | Patrones GoF adicionales de la 3.ª iteración |

> **Qué se migra del Corte 1:** la tabla `review_assignments`, `ReviewService` y `GUIAssignReviewers` (HU4 del Corte 1) **no se migran** al `question-service` en este corte. Quedan congelados en la rama del Corte 1 y se rediseñan dentro de HU05 en el Corte 3. El estado `Eliminada` del Corte 1 pasa a ser `Archivada` (RNF-16, sin borrado físico).

## 2. Mapa de servicios

| Servicio | Etiqueta | Puerto interno | Base de datos | Estilo interno | Responsabilidad (límite del contexto) | NO es responsable de |
|---|---|---|---|---|---|---|
| `api-gateway` | — | **8080** (único expuesto al host) | — | Spring Cloud Gateway (reactivo) | Enrutar por prefijo, validar firma/expiración del JWT, rechazar rutas por rol, CORS, propagar identidad en cabeceras `X-User-*` | Emitir tokens, lógica de negocio |
| `user-service` | `svc:usuarios` | 8081 | `db_users` | Capas (controller / service / repository) | Identidad: registro, login, emisión de JWT, gestión de usuarios y roles, auditoría de logins y de acciones administrativas | Contenido académico |
| `question-service` | `svc:preguntas` | 8082 | `db_questions` | Capas + Microkernel/Plugins + **State** | Ciclo de vida de la pregunta: creación (manual o por plugin), validación estructural, edición en Borrador, filtros, transiciones de estado, historial de cambios; **fuente de verdad** de preguntas | Armar simulacros, calificar |
| `simulation-service` | `svc:simulacros` | 8083 | `db_simulations` | **Hexagonal** (`domain`, `application`, `infrastructure`) | Réplica local de preguntas publicadas, generación aleatoria (**Strategy**), control de tiempo en servidor, calificación por competencia | Editar preguntas, estadísticas históricas |
| `tracking-service` | `svc:seguimiento` | 8084 | `db_tracking` | **Hexagonal** | Historial por estudiante, estadísticas, fortalezas/debilidades, reportes agregados para docentes | Calificar (recibe resultados ya calculados) |
| `common-events` | — | — | — | Librería Maven (JAR) | Contratos de eventos (records Java), nombres de exchange/routing keys, interfaces `EventPublisher` / `EventConsumer` | Ninguna lógica de negocio |
| `swing-client` | — | — | — | MVC + Observer | UI de escritorio; consume solo `http://localhost:8080/api/**` | Acceso directo a BD o a servicios internos |
| `rabbitmq` | — | 5672 / 15672 (UI) | — | Broker | Transporte asíncrono de eventos de dominio | — |

**Regla DoD:** cada servicio corre en su contenedor, usa **solo** su base PostgreSQL (usuario y password propios) y se comunica con los demás **solo por REST (a través del gateway) o por eventos**. Ningún servicio hace JOIN ni consulta la BD de otro. Los identificadores externos (`author_id`, `student_id`, `question_id`) se guardan como UUID **sin FK física**.

## 3. Seguridad transversal (A06, A10)

| Aspecto | Decisión |
|---|---|
| Emisor del token | `user-service` en `POST /api/auth/login` |
| Algoritmo | **HS256** con secreto compartido por variable de entorno `JWT_SECRET` (≥ 256 bits). Migrable a RS256 + JWKS en Corte 3 sin cambiar contratos |
| Claims | `sub` = userId (UUID), `login`, `role`, `name`, `iat`, `exp` (60 min) |
| Gateway | Valida firma y expiración. Rutas públicas: `POST /api/auth/register`, `POST /api/auth/login`, `GET /actuator/health`. Elimina cualquier `X-User-*` que llegue del cliente y reinyecta `X-User-Id`, `X-User-Login`, `X-User-Role` desde el token |
| Servicios | Configurados como *resource server* con el mismo secreto (defensa en profundidad) + `@PreAuthorize` por rol |
| Contraseñas | **BCrypt** (`BCryptPasswordEncoder`, cost 10–12) — RNF-08. *Cambio respecto al Corte 1, que usaba Argon2id:* los usuarios del Corte 1 no se migran; se crean semillas nuevas con Flyway |
| Registro | **Corrección del Corte 1:** el registro público ya **no** permite elegir rol (en `RegisterFrame` se podía escoger *Administrador*). Toda cuenta nueva nace como `ESTUDIANTE`/`ACTIVO`; los demás roles los asigna un Administrador (RF-03). El primer admin lo crea `AdminBootstrap` al arrancar, con `ADMIN_LOGIN`/`ADMIN_PASSWORD` del `.env` (sin credenciales en SQL) |
| Errores | Formato único RFC 7807 (`application/problem+json`): `401` token ausente/inválido, `403` rol insuficiente, `409` transición de estado inválida, `422` validación de negocio |

Roles (enum compartido en `common-events`): `ADMINISTRADOR`, `AUTOR`, `REVISOR`, `DOCENTE`, `ESTUDIANTE`. *Coordinador* se modela como `DOCENTE` en el Corte 2.

## 4. Rutas del gateway

| Prefijo | Destino | Roles permitidos (filtro grueso en gateway) |
|---|---|---|
| `/api/auth/**` | `user-service:8081` | Público |
| `/api/users/**` | `user-service:8081` | Autenticado (detalle por endpoint) |
| `/api/questions/**`, `/api/plugins/**` | `question-service:8082` | Autenticado |
| `/api/simulations/**` | `simulation-service:8083` | `ESTUDIANTE`, `ADMINISTRADOR` |
| `/api/tracking/**` | `tracking-service:8084` | Autenticado (detalle por endpoint) |

## 5. Endpoints REST

Todos bajo el gateway `http://localhost:8080`. Paginación estándar: `?page=0&size=20&sort=campo,asc` → respuesta `{content, page, size, totalElements, totalPages}`.

### 5.1 `user-service` (svc:usuarios)

| Método | Ruta | Rol | Requisito | Task | Descripción |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | Público | RF-01, RNF-08 | A08 | Crea usuario `ESTUDIANTE`. Body: `login, fullName, email, password`. Valida política de contraseña. `201` |
| POST | `/api/auth/login` | Público | RF-02, RNF-09 | A08, A10, A11 | Body: `login, password`. Devuelve `{accessToken, tokenType, expiresIn, user{id, login, fullName, role}}`. Registra el intento (éxito o fallo) en `login_audit` |
| GET | `/api/users/me` | Autenticado | RF-02 | A10 | Perfil del usuario del token |
| GET | `/api/users` | ADMINISTRADOR | RF-03 | A09 | Lista paginada. Filtros: `role`, `status`, `q` (login/nombre) |
| GET | `/api/users/{id}` | ADMINISTRADOR | RF-03 | A09 | Detalle |
| PATCH | `/api/users/{id}/status` | ADMINISTRADOR | RF-03 | A09 | Body: `{status: ACTIVO\|INACTIVO}`. Un admin no puede inactivarse a sí mismo |
| PATCH | `/api/users/{id}/role` | ADMINISTRADOR | RF-03 | A09 | Body: `{role}`. Se audita en `user_admin_audit` |
| GET | `/api/users/{id}/admin-audit` | ADMINISTRADOR | RF-03, RNF-09 | A09 | Historial de cambios de rol y estado de un usuario |
| GET | `/api/users/audit/logins` | ADMINISTRADOR | RNF-09 | A11 | Consulta paginada de `login_audit` (filtros `login`, `success`, `from`, `to`). *Panel avanzado de auditoría global → Corte 3* |

### 5.2 `question-service` (svc:preguntas)

| Método | Ruta | Rol | Requisito | Task | Descripción |
|---|---|---|---|---|---|
| POST | `/api/questions` | AUTOR, ADMINISTRADOR | RF-05, RF-08–RF-13 | A12, A15 | Crea en `BORRADOR`. Ejecuta la cadena de validadores. `201` / `422` con lista de errores |
| GET | `/api/questions` | Autenticado (no ESTUDIANTE) | RF-07 | A16 | Filtros dinámicos (JPA Specifications): `state, competence, topic, subtopic, difficulty, type, authorId, q`. AUTOR ve solo las suyas salvo `?scope=all` (ADMIN/REVISOR/DOCENTE) |
| GET | `/api/questions/mine` | AUTOR | RF-07 | A16 | Atajo de "mis preguntas" (equivale a HU3 del Corte 1) |
| GET | `/api/questions/{id}` | Autenticado (no ESTUDIANTE) | RF-05 | A12 | Detalle con opciones |
| PUT | `/api/questions/{id}` | AUTOR dueño | RF-06 | A16, A17 | Solo si estado = `BORRADOR` (si no → `409`). Revalida y registra cada campo cambiado en `question_change_history` |
| POST | `/api/questions/{id}/submit` | AUTOR dueño | RF-14 | A13 | `BORRADOR → PENDIENTE_REVISION` |
| POST | `/api/questions/{id}/start-review` | ADMINISTRADOR, REVISOR | RF-14 | A19 | `PENDIENTE_REVISION → EN_REVISION` (**mínimo**, sin asignación) |
| POST | `/api/questions/{id}/approve` | ADMINISTRADOR, REVISOR | RF-15 | A19 | `EN_REVISION → APROBADA`. Body opcional `{comment}` |
| POST | `/api/questions/{id}/reject` | ADMINISTRADOR, REVISOR | RF-15 | A19 | `EN_REVISION → RECHAZADA`. Body obligatorio `{comment}` |
| POST | `/api/questions/{id}/return-to-draft` | AUTOR dueño | RF-14 | A13 | `RECHAZADA → BORRADOR` (para corregir) |
| POST | `/api/questions/{id}/publish` | ADMINISTRADOR | RF-15 | A19, A18 | `APROBADA → PUBLICADA`. **Emite `PreguntaPublicada`** |
| POST | `/api/questions/{id}/archive` | ADMINISTRADOR (o AUTOR dueño si `BORRADOR`) | RNF-16 | A13, A18 | `→ ARCHIVADA`. Reemplaza al DELETE. Si venía de `PUBLICADA` **emite `PreguntaArchivada`** |
| GET | `/api/questions/{id}/history` | AUTOR dueño, ADMINISTRADOR | RF-29 | A17 | Cambios de campos (quién, cuándo, antes/después) |
| GET | `/api/questions/{id}/state-audit` | AUTOR dueño, ADMINISTRADOR | RF-30, RNF-09 | A17 | Transiciones de estado |
| GET | `/api/questions/stats/by-state` | ADMINISTRADOR | — | A16 | Conteo por estado (dashboard heredado del Corte 1) |
| GET | `/api/plugins` | AUTOR, ADMINISTRADOR | — | A14 | Plugins cargados: `MULTIPLE_CHOICE`, `CASE`, `MULTIMEDIA` |
| POST | `/api/plugins/{pluginId}/questions` | AUTOR, ADMINISTRADOR | RF-05, RF-08–RF-13 | A14, A15 | Genera la pregunta con el plugin, la pasa por los **mismos** validadores y la **persiste** en `BORRADOR` |

> **Corte 3 (no implementar):** `POST /api/questions/{id}/reviewers`, `POST /api/reviews/{id}/evaluation`, `GET /api/questions/{id}/reviews` (HU05, RF-16–RF-20). Se dejan reservados en la tabla para que la ruta `/api/reviews/**` no colisione.

#### Máquina de estados (patrón State, A13)

| Estado actual | Acción → Estado siguiente | ¿Editable? |
|---|---|---|
| `BORRADOR` | `submit` → `PENDIENTE_REVISION`; `archive` → `ARCHIVADA` | **Sí** (único estado editable, RF-06) |
| `PENDIENTE_REVISION` | `start-review` → `EN_REVISION`; `archive` → `ARCHIVADA` | No |
| `EN_REVISION` | `approve` → `APROBADA`; `reject` → `RECHAZADA` | No |
| `RECHAZADA` | `return-to-draft` → `BORRADOR`; `archive` → `ARCHIVADA` | No |
| `APROBADA` | `publish` → `PUBLICADA`; `archive` → `ARCHIVADA` | No |
| `PUBLICADA` | `archive` → `ARCHIVADA` (emite `PreguntaArchivada`) | No |
| `ARCHIVADA` | — (estado final) | No |

Cualquier otra combinación lanza `InvalidStateTransitionException` → HTTP `409`.

### 5.3 `simulation-service` (svc:simulacros)

| Método | Ruta | Rol | Requisito | Task | Descripción |
|---|---|---|---|---|---|
| GET | `/api/simulations/catalog` | ESTUDIANTE | RF-21 | B06 | Competencias, temas y dificultades con conteo de preguntas `AVAILABLE` en la réplica (para que el cliente no pida imposibles) |
| POST | `/api/simulations` | ESTUDIANTE | RF-21, RF-22, RF-23 | B06, B07 | Body: `{strategy, competence?, topic?, difficulty?, questionCount, timeLimitMinutes}`. Selecciona preguntas con la **Strategy** elegida, fija `startedAt` y `expiresAt` en servidor. Devuelve preguntas **sin** respuesta correcta. `422` si no hay suficientes preguntas |
| GET | `/api/simulations/{id}` | ESTUDIANTE dueño | RF-23 | B07 | Estado actual + `serverTime` + `remainingSeconds` (para sincronizar el cronómetro del cliente) |
| PUT | `/api/simulations/{id}/answers/{questionId}` | ESTUDIANTE dueño | RF-23 | B07 | Body: `{selectedOption: A-D}`. Rechazado con `409` si `now > expiresAt` o el simulacro ya terminó |
| POST | `/api/simulations/{id}/finish` | ESTUDIANTE dueño | RF-24 | B08, B09 | Califica, guarda puntaje por competencia y **emite `SimulacroFinalizado`**. Devuelve resultado inmediato con justificaciones |
| GET | `/api/simulations/{id}/result` | ESTUDIANTE dueño | RF-24 | B08 | Resultado de un simulacro terminado |
| GET | `/api/simulations/mine/active` | ESTUDIANTE | RF-23 | B07 | Simulacro en curso (para retomar tras cerrar la app) |

> **Expiración (RF-23):** un `@Scheduled` cada 30 s cierra los simulacros `IN_PROGRESS` con `expiresAt < now`, los califica con las respuestas guardadas, los marca `EXPIRED` y también emite `SimulacroFinalizado`. El tiempo **nunca** se toma del cliente.

### 5.4 `tracking-service` (svc:seguimiento)

| Método | Ruta | Rol | Requisito | Task | Descripción |
|---|---|---|---|---|---|
| GET | `/api/tracking/students/me/history` | ESTUDIANTE | RF-25 | B11 | Historial paginado de simulacros propios |
| GET | `/api/tracking/students/me/stats` | ESTUDIANTE | RF-26, RF-27 | B12 | Promedio global y por competencia, tendencia, **fortalezas y debilidades** |
| GET | `/api/tracking/students/{studentId}/history` | DOCENTE, ADMINISTRADOR | RF-25 | B11 | Historial de un estudiante |
| GET | `/api/tracking/students/{studentId}/stats` | DOCENTE, ADMINISTRADOR | RF-26, RF-27 | B12 | Estadísticas de un estudiante |
| GET | `/api/tracking/reports/competences` | DOCENTE, ADMINISTRADOR | RF-28 | B13 | Rendimiento agregado por competencia (`from`, `to`) |
| GET | `/api/tracking/reports/students` | DOCENTE, ADMINISTRADOR | RF-28 | B13 | Ranking/listado de estudiantes con promedio y nº de simulacros |
| GET | `/api/tracking/reports/topics` | DOCENTE, ADMINISTRADOR | RF-28 | B13 | Temas con menor tasa de acierto |

**Regla de clasificación (RF-27), parametrizable en `application.yml`:** por competencia, con al menos 5 preguntas respondidas en total: `FORTALEZA` si promedio ≥ 70 %, `DEBILIDAD` si promedio < 50 %, `EN_DESARROLLO` en otro caso.

## 6. Eventos de dominio (RabbitMQ)

### 6.1 Topología

| Elemento | Nombre | Tipo / configuración |
|---|---|---|
| Exchange principal | `saberpro.events` | `topic`, durable |
| Dead-letter exchange | `saberpro.events.dlx` | `topic`, durable |
| Cola | `simulation.question-published.q` | durable, binding `question.published`, DLQ `simulation.question-published.dlq` |
| Cola | `simulation.question-archived.q` | durable, binding `question.archived`, DLQ `simulation.question-archived.dlq` |
| Cola | `tracking.simulation-finished.q` | durable, binding `simulation.finished`, DLQ `tracking.simulation-finished.dlq` |

Convención: la **cola pertenece al consumidor** (`<servicio-consumidor>.<evento>.q`) y la declara el consumidor; el productor solo conoce el exchange y la routing key. Así un nuevo consumidor (p. ej. un servicio de notificaciones en Corte 3) se agrega sin tocar al productor.

### 6.2 Catálogo de eventos

| Evento | Routing key | Productor | Consumidores | Cuándo se emite | Task |
|---|---|---|---|---|---|
| `PreguntaPublicada` | `question.published` | question-service | simulation-service | Transición `APROBADA → PUBLICADA` | A18 (prod.), B05 (cons.) |
| `PreguntaArchivada` | `question.archived` | question-service | simulation-service | Transición `PUBLICADA → ARCHIVADA` | A18 (prod.), B05 (cons.) |
| `SimulacroFinalizado` | `simulation.finished` | simulation-service | tracking-service | `finish` del estudiante o expiración en servidor | B09 (prod.), B10 (cons.) |

### 6.3 Sobre común (`EventEnvelope<T>` en `common-events`)

```json
{
  "eventId": "8b1c2f9e-3a77-4d0b-9c1e-2f5a6d7e8f90",
  "eventType": "PreguntaPublicada",
  "eventVersion": 1,
  "occurredAt": "2026-11-23T22:15:03Z",
  "producer": "question-service",
  "correlationId": "req-5f2a…",
  "payload": { }
}
```

### 6.4 Payloads

**`PreguntaPublicada` (v1)** — lleva todo lo que el simulador necesita para evaluar sin volver a llamar al `question-service`:

```json
{
  "questionId": "c0a8012e-…",
  "code": "Q-2026-000123",
  "version": 4,
  "type": "MULTIPLE_CHOICE",
  "context": "Una empresa de software…",
  "statement": "¿Cuál patrón…?",
  "options": [
    {"label": "A", "text": "…"}, {"label": "B", "text": "…"},
    {"label": "C", "text": "…"}, {"label": "D", "text": "…"}
  ],
  "correctOption": "B",
  "justification": "…",
  "competence": "Diseño de software",
  "topic": "Patrones de diseño",
  "subtopic": "Comportamiento",
  "difficulty": "MEDIA",
  "publishedAt": "2026-11-23T22:15:03Z"
}
```

**`PreguntaArchivada` (v1)**

```json
{ "questionId": "c0a8012e-…", "version": 5, "archivedAt": "2026-11-24T14:00:00Z", "archivedBy": "admin" }
```

**`SimulacroFinalizado` (v1)**

```json
{
  "simulationId": "5d9e…",
  "studentId": "a1b2…",
  "studentLogin": "jdiaz",
  "finalStatus": "FINISHED",
  "strategy": "COMPETENCE",
  "startedAt": "2026-11-25T15:00:00Z",
  "finishedAt": "2026-11-25T15:41:12Z",
  "totalQuestions": 20,
  "correctAnswers": 14,
  "globalScore": 70.0,
  "competenceScores": [
    {"competence": "Diseño de software", "total": 10, "correct": 8, "score": 80.0},
    {"competence": "Razonamiento cuantitativo", "total": 10, "correct": 6, "score": 60.0}
  ],
  "answers": [
    {"questionId": "c0a8…", "competence": "Diseño de software", "topic": "Patrones de diseño",
     "difficulty": "MEDIA", "selectedOption": "B", "correct": true}
  ]
}
```

### 6.5 Garantías de entrega

| Garantía | Cómo se logra |
|---|---|
| No perder eventos si RabbitMQ está caído | **Transactional Outbox:** el cambio de estado y la fila en `outbox_events` se guardan en la misma transacción; un `@Scheduled` (cada 2 s) publica las pendientes con *publisher confirms* y marca `published_at` |
| Mensajes persistentes | Colas/exchanges durables, `deliveryMode = PERSISTENT` |
| Reintentos | Spring AMQP retry: 3 intentos con backoff exponencial (1 s, 2 s, 4 s); luego `reject` → DLQ |
| Idempotencia (entrega *at-least-once*) | Cada consumidor guarda `eventId` en `processed_events` dentro de la misma transacción; si ya existe, ignora |
| Orden / eventos viejos | La réplica guarda `source_version`; un evento con versión menor o igual se descarta |
| Serialización | JSON con `Jackson2JsonMessageConverter`; contratos versionados (`eventVersion`) |

## 7. Trazabilidad backlog ↔ diseño

| Task | Dónde queda reflejado |
|---|---|
| A08–A11 | §3, §5.1, ER `db_users` |
| A12–A17 | §5.2, máquina de estados, ER `db_questions` |
| A18, A19 | §5.2 (`publish`, `archive`, `start-review`, `approve`, `reject`), §6 |
| B05–B09 | §5.3, §6, ER `db_simulations` |
| B10–B13 | §5.4, §6, ER `db_tracking` |
