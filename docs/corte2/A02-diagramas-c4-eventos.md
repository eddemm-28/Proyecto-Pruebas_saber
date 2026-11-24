# A02 — Diagramas C4 (niveles 1 y 2) y flujo de eventos

> **Corte 2 · Sprint A · Diseño Ronda 1** · Responsable: Integrante 3 · Talla S
> Fuente editable: carpeta [`diagramas/`](diagramas/). Imágenes generadas con `mmdc` en [`render/`](render/). GitHub renderiza los bloques `mermaid` de este archivo directamente.

## 1. C4 Nivel 1 — Contexto

Archivo: [`diagramas/c4/c4-nivel1-contexto.mmd`](diagramas/c4/c4-nivel1-contexto.mmd)

```mermaid
C4Context
    title C4 Nivel 1 - Contexto: Banco de Preguntas Saber Pro (Corte 2)

    Person(admin, "Administrador", "Gestiona usuarios, roles y aprueba/publica preguntas (endpoint mínimo A19).")
    Person(autor, "Autor de preguntas", "Crea y edita preguntas en Borrador y las envía a revisión.")
    Person(revisor, "Revisor", "Rol reservado. La revisión por pares completa (HU05) llega en el Corte 3.")
    Person(estudiante, "Estudiante", "Presenta simulacros cronometrados y consulta su historial y estadísticas.")
    Person(docente, "Docente / Coordinador", "Consulta reportes agregados de rendimiento.")

    System(sistema, "Sistema Banco de Preguntas Saber Pro", "Gestiona, valida y administra el banco de preguntas; genera y califica simulacros y hace seguimiento académico.")

    System_Ext(smtp, "Servidor SMTP", "Envío de correos. Opcional en Corte 2, heredado del Corte 1.")

    Rel(admin, sistema, "Administra usuarios y publica preguntas", "Cliente Swing / HTTPS")
    Rel(autor, sistema, "Crea, edita y envía preguntas a revisión", "Cliente Swing / HTTPS")
    Rel(revisor, sistema, "Revisa preguntas (Corte 3)", "Cliente Swing / HTTPS")
    Rel(estudiante, sistema, "Presenta simulacros y consulta resultados", "Cliente Swing / HTTPS")
    Rel(docente, sistema, "Consulta reportes", "Cliente Swing / HTTPS")
    Rel(sistema, smtp, "Envía notificaciones", "SMTP")

    UpdateLayoutConfig($c4ShapeInRow="5", $c4BoundaryInRow="1")
```

**Lectura:** cinco actores usan un único sistema a través del cliente Swing. El **Revisor** aparece porque el rol ya existe (RF-03), pero su flujo completo (HU05) es del Corte 3. El SMTP heredado del Corte 1 queda como sistema externo opcional.

## 2. C4 Nivel 2 — Contenedores

Archivo: [`diagramas/c4/c4-nivel2-contenedores.mmd`](diagramas/c4/c4-nivel2-contenedores.mmd)

```mermaid
C4Container
    title C4 Nivel 2 - Contenedores: Banco de Preguntas Saber Pro (Corte 2)

    Person(usuario, "Usuario", "Administrador, Autor, Revisor, Docente o Estudiante")

    Container(swing, "swing-client", "Java 17, Swing, MVC + Observer, java.net.http.HttpClient", "Aplicación de escritorio. Solo conoce la URL del gateway y guarda el JWT en memoria.")

    System_Boundary(backend, "Backend - red Docker interna (saberpro-net)") {
        Container(gateway, "api-gateway", "Spring Cloud Gateway, Spring Security (JWT)", "Único punto de entrada (:8080). Enruta por prefijo, valida el JWT y propaga X-User-Id, X-User-Login y X-User-Role.")
        ContainerQueue(rabbit, "RabbitMQ", "RabbitMQ 3.13 + management", "Exchange topic saberpro.events y DLX saberpro.events.dlx")

        Container_Boundary(bUsers, "svc:usuarios") {
            Container(users, "user-service", "Spring Boot 3, Spring Security, JPA, BCrypt", "Registro, login, emisión de JWT, CRUD de usuarios y auditoría de logins.")
            ContainerDb(dbUsers, "db_users", "PostgreSQL 16", "users, login_audit, user_admin_audit")
        }
        Container_Boundary(bQuestions, "svc:preguntas") {
            Container(questions, "question-service", "Spring Boot 3, JPA, Spring AMQP", "Banco de preguntas, State, validadores, Microkernel/Plugins, historial y aprobación mínima (A19).")
            ContainerDb(dbQuestions, "db_questions", "PostgreSQL 16", "questions, question_options, historial, auditoría, outbox")
        }
        Container_Boundary(bSimulations, "svc:simulacros (hexagonal)") {
            Container(simulations, "simulation-service", "Spring Boot 3, Spring AMQP", "Réplica de preguntas publicadas, generación (Strategy), cronómetro en servidor y calificación.")
            ContainerDb(dbSimulations, "db_simulations", "PostgreSQL 16", "question_replica, simulations, simulation_items, competence_scores")
        }
        Container_Boundary(bTracking, "svc:seguimiento (hexagonal)") {
            Container(tracking, "tracking-service", "Spring Boot 3, Spring AMQP", "Historial, estadísticas, fortalezas/debilidades y reportes agregados.")
            ContainerDb(dbTracking, "db_tracking", "PostgreSQL 16", "simulation_results, student_competence_stats")
        }
    }

    Rel(usuario, swing, "Usa")
    Rel(swing, gateway, "Llamadas REST + JWT Bearer", "JSON/HTTP :8080")

    Rel(gateway, users, "/api/auth/**, /api/users/**", "HTTP :8081")
    Rel(gateway, questions, "/api/questions/**, /api/plugins/**", "HTTP :8082")
    Rel(gateway, simulations, "/api/simulations/**", "HTTP :8083")
    Rel(gateway, tracking, "/api/tracking/**", "HTTP :8084")

    Rel_D(users, dbUsers, "Lee/escribe", "JPA/JDBC")
    Rel_D(questions, dbQuestions, "Lee/escribe", "JPA/JDBC")
    Rel_D(simulations, dbSimulations, "Lee/escribe", "JPA/JDBC")
    Rel_D(tracking, dbTracking, "Lee/escribe", "JPA/JDBC")

    Rel(questions, rabbit, "Publica PreguntaPublicada, PreguntaArchivada", "AMQP")
    Rel(rabbit, simulations, "Entrega eventos de preguntas", "AMQP")
    Rel(simulations, rabbit, "Publica SimulacroFinalizado", "AMQP")
    Rel(rabbit, tracking, "Entrega SimulacroFinalizado", "AMQP")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")
```

**Decisiones que muestra el diagrama:**

- El cliente Swing **solo** conoce el gateway (`:8080`); los servicios y las BD no publican puertos al host (salvo en el perfil `dev` de Docker Compose, para depurar).
- Cada servicio está encerrado con **su** base PostgreSQL: no hay flechas entre un servicio y la BD de otro.
- La comunicación entre servicios es **solo asíncrona** (RabbitMQ). No hay llamadas REST servicio-a-servicio en el Corte 2: el `simulation-service` mantiene una réplica local de preguntas en vez de consultar al `question-service` en cada simulacro.

> *Nota:* el motor C4 de Mermaid tiene diseño automático limitado y algunas etiquetas se cruzan. Para la versión del PDF (B19) se puede exportar el PNG de `render/` o redibujar en PlantUML C4 si se necesita control fino de la posición.

## 3. Diagrama de flujo de eventos

Archivo: [`diagramas/eventos/flujo-eventos.mmd`](diagramas/eventos/flujo-eventos.mmd)

```mermaid
flowchart LR
    %% Diagrama de flujo de eventos - Corte 2 (Ronda 1)

    subgraph QS["question-service (svc:preguntas)"]
        Q1["POST /api/questions/{id}/publish<br/>Aprobada -> Publicada"]
        Q2["POST /api/questions/{id}/archive<br/>* -> Archivada"]
        QO[("outbox_events<br/>db_questions")]
        Q1 --> QO
        Q2 --> QO
    end

    subgraph MQ["RabbitMQ"]
        EX{{"Exchange topic<br/>saberpro.events"}}
        QA[["simulation.question-published.q"]]
        QB[["simulation.question-archived.q"]]
        QC[["tracking.simulation-finished.q"]]
        DLX{{"DLX saberpro.events.dlx"}}
        DLQ[["*.dlq<br/>(una por cola)"]]
        EX -- "question.published" --> QA
        EX -- "question.archived" --> QB
        EX -- "simulation.finished" --> QC
        QA -. "rechazo tras 3 reintentos" .-> DLX
        QB -.-> DLX
        QC -.-> DLX
        DLX --> DLQ
    end

    subgraph SS["simulation-service (svc:simulacros)"]
        S1["Consumer: upsert question_replica<br/>status = AVAILABLE"]
        S2["Consumer: question_replica<br/>status = ARCHIVED"]
        S3["POST /api/simulations/{id}/finish<br/>o expiración en servidor"]
        SO[("outbox_events<br/>db_simulations")]
        S3 --> SO
    end

    subgraph TS["tracking-service (svc:seguimiento)"]
        T1["Consumer: guarda simulation_results<br/>y recalcula student_competence_stats"]
    end

    QO -- "PreguntaPublicada" --> EX
    QO -- "PreguntaArchivada" --> EX
    QA --> S1
    QB --> S2
    SO -- "SimulacroFinalizado" --> EX
    QC --> T1

    classDef evt fill:#fff4d6,stroke:#b7791f,color:#000
    class EX,DLX evt
```

**Flujo crítico** (se verificará end-to-end en B17):

1. Un administrador publica una pregunta aprobada → `question-service` guarda el cambio de estado y la fila del outbox en la misma transacción.
2. El *relay* del outbox publica `PreguntaPublicada` en `saberpro.events` con routing key `question.published`.
3. `simulation-service` la consume y hace *upsert* en `question_replica` (idempotente por `eventId` y `source_version`).
4. Un estudiante genera, responde y finaliza un simulacro (o expira) → `SimulacroFinalizado` sale por el outbox del `simulation-service`.
5. `tracking-service` lo consume, guarda el resultado y recalcula `student_competence_stats`.
6. Tras 3 reintentos fallidos, el mensaje va a la DLQ de su cola para inspección manual en la UI de RabbitMQ (`:15672`).

> **B04 (Sprint B):** este diagrama ya incluye `SimulacroFinalizado`. En la Ronda 2 solo habrá que actualizarlo si cambian nombres o se agregan consumidores.
