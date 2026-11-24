# A03 — Modelo Entidad-Relación por base de datos

> **Corte 2 · Sprint A · Diseño Ronda 1** · Responsable: Integrante 3 · Talla M
> Una base PostgreSQL 16 **independiente** por servicio (una instancia/contenedor por BD en Docker Compose). Fuentes: [`diagramas/er/`](diagramas/er/). Las migraciones se implementan con **Flyway** (`src/main/resources/db/migration/V1__init.sql` en cada servicio) y `spring.jpa.hibernate.ddl-auto=validate`.

## Convenciones comunes

| Convención | Regla |
|---|---|
| Claves primarias | `UUID` generados en la aplicación (`UUID.randomUUID()`), salvo tablas de auditoría/log (`BIGSERIAL`) |
| Fechas | `TIMESTAMPTZ` en UTC |
| Enumeraciones | `VARCHAR` + `CHECK` (no tipos `ENUM` nativos, para que Flyway y JPA `@Enumerated(STRING)` sean simples) |
| Referencias entre servicios | Columnas `*_id` de tipo UUID **sin FK física** (otra BD). Se copia lo necesario para mostrar (`*_login`) — duplicación deliberada |
| Concurrencia | Columna `version` + `@Version` (bloqueo optimista) en agregados editables |
| Borrado | Sin `DELETE` de negocio: estados `ARCHIVADA`, `INACTIVO`, `ARCHIVED` (RNF-16) |
| Idempotencia de eventos | Tabla `processed_events` en cada consumidor |
| Publicación confiable | Tabla `outbox_events` en cada productor |

## 1. `db_users` — user-service

```mermaid
erDiagram
    %% db_users - user-service (svc:usuarios)

    USERS {
        uuid id PK
        varchar login UK "3-50 caracteres"
        varchar full_name "NOT NULL"
        varchar email UK "NOT NULL"
        varchar password_hash "BCrypt, cost 10-12"
        varchar role "ADMINISTRADOR | AUTOR | REVISOR | DOCENTE | ESTUDIANTE"
        varchar status "ACTIVO | INACTIVO"
        timestamptz created_at
        timestamptz updated_at
        bigint version "bloqueo optimista"
    }

    LOGIN_AUDIT {
        bigserial id PK
        uuid user_id FK "NULL si el login no existe"
        varchar login_attempted "NOT NULL"
        boolean success
        varchar failure_reason "BAD_CREDENTIALS | USER_INACTIVE | USER_NOT_FOUND"
        varchar ip_address
        varchar client_agent
        timestamptz occurred_at
    }

    USER_ADMIN_AUDIT {
        bigserial id PK
        uuid target_user_id FK
        uuid actor_user_id FK
        varchar action "ROLE_CHANGED | ACTIVATED | INACTIVATED"
        varchar old_value
        varchar new_value
        timestamptz occurred_at
    }

    USERS ||--o{ LOGIN_AUDIT : "registra intentos de"
    USERS ||--o{ USER_ADMIN_AUDIT : "es objeto de"
    USERS ||--o{ USER_ADMIN_AUDIT : "ejecuta como admin"
```

| Tabla | Propósito | Requisito |
|---|---|---|
| `users` | Cuenta, rol único y estado. Índices: `UNIQUE(login)`, `UNIQUE(email)`, `(role, status)` | RF-01, RF-03, RNF-08 |
| `login_audit` | Cada intento de login, exitoso o no, con IP y motivo de fallo. Índice `(login_attempted, occurred_at)` | RNF-09, A11 |
| `user_admin_audit` | Cambios de rol y activación/inactivación hechos por un administrador | RF-03, RNF-09 |

## 2. `db_questions` — question-service

```mermaid
erDiagram
    %% db_questions - question-service (svc:preguntas)

    QUESTIONS {
        uuid id PK
        varchar code UK "ej. Q-2026-000123, legible para humanos"
        varchar title "antes: nombre"
        varchar question_type "MULTIPLE_CHOICE | CASE | MULTIMEDIA"
        text context "RF-08 obligatorio"
        text statement "pregunta directa"
        char correct_option "A | B | C | D"
        text justification
        text bibliography
        varchar competence
        varchar topic
        varchar subtopic
        varchar difficulty "FACIL | MEDIA | DIFICIL"
        varchar state "7 estados del patrón State"
        uuid author_id "id del user-service, sin FK física"
        varchar author_login "copia para mostrar"
        varchar plugin_id "plugin que la generó o MANUAL"
        varchar media_url "solo MULTIMEDIA"
        timestamptz created_at
        timestamptz updated_at
        timestamptz published_at
        timestamptz archived_at
        bigint version "bloqueo optimista"
    }

    QUESTION_OPTIONS {
        uuid question_id PK, FK
        char label PK "A | B | C | D"
        text text "NOT NULL, únicas por pregunta"
    }

    QUESTION_CHANGE_HISTORY {
        bigserial id PK
        uuid question_id FK
        uuid changed_by "id del usuario"
        varchar changed_by_login
        varchar field_name
        text old_value
        text new_value
        timestamptz changed_at
    }

    QUESTION_STATE_AUDIT {
        bigserial id PK
        uuid question_id FK
        varchar from_state "NULL al crear"
        varchar to_state
        uuid changed_by
        varchar changed_by_login
        varchar comment
        timestamptz changed_at
    }

    OUTBOX_EVENTS {
        uuid id PK "= eventId del mensaje"
        uuid aggregate_id
        varchar event_type "PreguntaPublicada | PreguntaArchivada"
        varchar routing_key
        jsonb payload
        timestamptz created_at
        timestamptz published_at "NULL = pendiente"
        int attempts
        varchar last_error
    }

    QUESTIONS ||--|{ QUESTION_OPTIONS : "tiene exactamente 4"
    QUESTIONS ||--o{ QUESTION_CHANGE_HISTORY : "registra cambios (RF-29)"
    QUESTIONS ||--|{ QUESTION_STATE_AUDIT : "registra transiciones (RF-30)"
    QUESTIONS ||--o{ OUTBOX_EVENTS : "origina"
```

| Tabla | Propósito | Requisito |
|---|---|---|
| `questions` | Agregado raíz. `state` guarda el nombre del estado; la clase `QuestionState` concreta se reconstruye al cargar (patrón State). Índices: `(state)`, `(author_id)`, `(competence, topic, difficulty)` para los filtros dinámicos | RF-05, RF-07, RF-14, RF-15 |
| `question_options` | Exactamente 4 opciones A–D. `UNIQUE(question_id, lower(text))` impide opciones repetidas; `CHECK` sobre `label` | RF-10, RF-11 |
| `question_change_history` | Un registro por campo modificado en cada edición en Borrador | RF-29 |
| `question_state_audit` | Un registro por transición (incluida la creación `NULL → BORRADOR`) | RF-30, RNF-09 |
| `outbox_events` | Eventos `PreguntaPublicada` / `PreguntaArchivada` pendientes de enviar | A18 |

**Cambios respecto al SQLite del Corte 1:**

| Corte 1 (`questions` en SQLite) | Corte 2 (`db_questions`) | Motivo |
|---|---|---|
| `option_a … option_d` como columnas | Tabla `question_options` | Validación de unicidad en BD y el mismo formato para todos los plugins (A14) |
| `respuesta_correcta` texto libre (A–D o el texto de la opción, según el plugin) | `correct_option CHAR(1)` con `CHECK IN ('A','B','C','D')` | Estandarizar el formato de respuesta correcta (A14) |
| `estado` con 3 valores (`Borrador`, `Pendiente de revisión`, `Eliminada`) | 7 estados; `Eliminada` → `ARCHIVADA` | A13, RNF-16 |
| `autor` = login | `author_id` (UUID) + `author_login` | El id es estable aunque cambie el login |
| `id TEXT` | `id UUID` + `code` legible | Ids globales entre servicios |
| `nombre` | `title` | Nombres en inglés en el esquema; la UI sigue en español |
| `review_assignments` | **No se migra** | HU05 → Corte 3 |

> **Reservado para Corte 3 (HU05):** tablas `review_assignments`, `review_evaluations` y `review_observations` en `db_questions` (o en un futuro `review-service`). No se crean en las migraciones del Corte 2.

## 3. `db_simulations` — simulation-service (hexagonal)

```mermaid
erDiagram
    %% db_simulations - simulation-service (svc:simulacros, hexagonal)

    QUESTION_REPLICA {
        uuid question_id PK "id original del question-service"
        varchar code
        text context
        text statement
        char correct_option "A | B | C | D"
        text justification
        varchar competence
        varchar topic
        varchar subtopic
        varchar difficulty
        varchar status "AVAILABLE | ARCHIVED"
        bigint source_version "descarta eventos viejos"
        timestamptz published_at
        timestamptz synced_at
    }

    QUESTION_REPLICA_OPTIONS {
        uuid question_id PK, FK
        char label PK
        text text
    }

    SIMULATIONS {
        uuid id PK
        uuid student_id "id del user-service, sin FK física"
        varchar student_login
        varchar strategy "COMPETENCE | TOPIC | DIFFICULTY | MIXED"
        varchar filter_competence
        varchar filter_topic
        varchar filter_difficulty
        int question_count
        int time_limit_minutes
        varchar status "IN_PROGRESS | FINISHED | EXPIRED"
        timestamptz started_at
        timestamptz expires_at "calculado en el servidor (RF-23)"
        timestamptz finished_at
        int correct_count
        numeric global_score "0-100"
        bigint version
    }

    SIMULATION_ITEMS {
        uuid id PK
        uuid simulation_id FK
        uuid question_id FK
        int position
        varchar competence "snapshot"
        varchar topic "snapshot"
        varchar difficulty "snapshot"
        char correct_option "snapshot al generar"
        char selected_option "NULL = sin responder"
        boolean is_correct
        timestamptz answered_at
    }

    COMPETENCE_SCORES {
        uuid simulation_id PK, FK
        varchar competence PK
        int total_questions
        int correct_answers
        numeric score "0-100"
    }

    PROCESSED_EVENTS {
        uuid event_id PK
        varchar event_type
        timestamptz processed_at
    }

    OUTBOX_EVENTS {
        uuid id PK
        uuid aggregate_id
        varchar event_type "SimulacroFinalizado"
        varchar routing_key
        jsonb payload
        timestamptz created_at
        timestamptz published_at
        int attempts
        varchar last_error
    }

    QUESTION_REPLICA ||--|{ QUESTION_REPLICA_OPTIONS : "tiene 4"
    SIMULATIONS ||--|{ SIMULATION_ITEMS : "contiene"
    QUESTION_REPLICA ||--o{ SIMULATION_ITEMS : "se usa en"
    SIMULATIONS ||--o{ COMPETENCE_SCORES : "se califica por competencia"
    SIMULATIONS ||--o| OUTBOX_EVENTS : "emite al finalizar"
```

| Tabla | Propósito | Requisito |
|---|---|---|
| `question_replica` + `question_replica_options` | Proyección **de solo lectura** de las preguntas publicadas, alimentada por eventos. `status = ARCHIVED` las excluye de nuevos simulacros sin afectar los ya generados | B05 |
| `simulations` | Agregado raíz: configuración, estrategia, `started_at` / `expires_at` fijados por el servidor, estado y puntaje global | RF-21, RF-22, RF-23 |
| `simulation_items` | Una fila por pregunta del simulacro con **snapshot** de competencia, tema y respuesta correcta (si la pregunta se archiva después, el simulacro sigue calificable) | RF-23, RF-24 |
| `competence_scores` | Puntaje por competencia calculado al finalizar | RF-24 |
| `processed_events` / `outbox_events` | Idempotencia de consumo y publicación confiable de `SimulacroFinalizado` | B05, B09 |

En hexagonal estas tablas son detalles del adaptador `infrastructure/persistence` (entidades JPA `*JpaEntity`); el `domain` trabaja con `Simulation`, `SimulationItem`, `CompetenceScore` y `QuestionSnapshot` sin anotaciones JPA, y se traduce con *mappers*.

## 4. `db_tracking` — tracking-service (hexagonal)

```mermaid
erDiagram
    %% db_tracking - tracking-service (svc:seguimiento, hexagonal)

    SIMULATION_RESULTS {
        uuid simulation_id PK "id del simulation-service"
        uuid student_id
        varchar student_login
        varchar final_status "FINISHED | EXPIRED"
        varchar strategy
        int total_questions
        int correct_answers
        numeric global_score "0-100"
        int duration_seconds
        timestamptz started_at
        timestamptz finished_at
        timestamptz received_at
    }

    RESULT_COMPETENCE_SCORES {
        uuid simulation_id PK, FK
        varchar competence PK
        int total_questions
        int correct_answers
        numeric score
    }

    RESULT_ANSWERS {
        uuid simulation_id PK, FK
        uuid question_id PK
        varchar competence
        varchar topic
        varchar difficulty
        boolean answered
        boolean is_correct
    }

    STUDENT_COMPETENCE_STATS {
        uuid student_id PK
        varchar competence PK
        varchar student_login
        int simulations_count
        int total_questions
        int correct_answers
        numeric average_score
        numeric last_score
        varchar classification "FORTALEZA | DEBILIDAD | EN_DESARROLLO"
        timestamptz updated_at
    }

    PROCESSED_EVENTS {
        uuid event_id PK
        varchar event_type
        timestamptz processed_at
    }

    SIMULATION_RESULTS ||--|{ RESULT_COMPETENCE_SCORES : "desglosa"
    SIMULATION_RESULTS ||--|{ RESULT_ANSWERS : "detalla"
    SIMULATION_RESULTS }o--|| STUDENT_COMPETENCE_STATS : "actualiza (por student_id + competence)"
```

| Tabla | Propósito | Requisito |
|---|---|---|
| `simulation_results` | Una fila por `SimulacroFinalizado` recibido (PK = `simulation_id`, garantiza idempotencia natural) | RF-25 |
| `result_competence_scores` | Desglose por competencia de cada resultado | RF-26 |
| `result_answers` | Detalle por pregunta (sin texto) para reportes por tema y dificultad | RF-28 |
| `student_competence_stats` | Proyección acumulada por estudiante y competencia; se recalcula al consumir cada evento. Fuente directa de fortalezas/debilidades | RF-26, RF-27 |
| `processed_events` | Idempotencia de consumo | B10 |

La relación `SIMULATION_RESULTS → STUDENT_COMPETENCE_STATS` es **lógica** (se actualiza por `student_id + competence`), no una FK.
