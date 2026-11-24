# B19 — Actualización final de la arquitectura (Ronda 2)

> **Corte 2 · Sprint B · Documentación final** · Responsable: Integrante 3 · Talla M
> Complementa A01–A03 (Ronda 1). Fuentes en [`diagramas/`](diagramas/); imágenes en [`render/`](render/).

## 1. C4 Nivel 3 — Componentes

| Servicio | Fuente | Imagen |
|---|---|---|
| question-service (capas + State + Microkernel) | [`c4-nivel3-question-service.mmd`](diagramas/c4/c4-nivel3-question-service.mmd) | [PNG](render/c4-nivel3-question-service.png) |
| simulation-service (hexagonal + Strategy) | [`c4-nivel3-simulation-service.mmd`](diagramas/c4/c4-nivel3-simulation-service.mmd) | [PNG](render/c4-nivel3-simulation-service.png) |
| tracking-service (hexagonal) | [`c4-nivel3-tracking-service.mmd`](diagramas/c4/c4-nivel3-tracking-service.mmd) | [PNG](render/c4-nivel3-tracking-service.png) |

## 2. Vistas 4+1 (PlantUML)

| Vista | Diagrama | Fuente |
|---|---|---|
| Escenarios (+1) | Historias HU06/HU07 y escenarios de calidad | [B01](B01-historias-usuario-HU06-HU07.md), [B03](B03-escenarios-calidad.md) |
| Lógica | Clases del patrón State | [`clases-patron-state.puml`](diagramas/uml/clases-patron-state.puml) |
| Lógica | Máquina de estados de la pregunta | [`estados-pregunta.puml`](diagramas/uml/estados-pregunta.puml) |
| Procesos | Secuencia del flujo crítico Publicar → Simulacro → Seguimiento | [`secuencia-flujo-critico.puml`](diagramas/uml/secuencia-flujo-critico.puml) |
| Desarrollo | Hexágono del simulador y patrón Strategy | [`clases-hexagonal-simulacros.puml`](diagramas/uml/clases-hexagonal-simulacros.puml) |
| Física | Despliegue Docker Compose | [`despliegue.puml`](diagramas/uml/despliegue.puml) |

Renderizar: `java -jar plantuml.jar -tpng docs/corte2/diagramas/uml/*.puml` o la extensión PlantUML de VS Code / IntelliJ.

## 3. ER actualizados

Los modelos de [A03](A03-modelo-entidad-relacion.md) coinciden con las migraciones `V1__init.sql` de cada servicio. Cambios respecto a la Ronda 1:

| BD | Cambio | Motivo |
|---|---|---|
| `db_questions`, `db_simulations` | `outbox_events.last_error` | Diagnóstico de reintentos del relay |
| `db_simulations` | Índice único parcial `uk_sim_one_active (student_id) WHERE status='IN_PROGRESS'` | Garantiza un solo simulacro activo aun con peticiones concurrentes |
| `db_simulations` | `simulation_items` con `UNIQUE (simulation_id, question_id)` | Sin preguntas repetidas en un simulacro |
| `db_users` | Admin inicial creado por `AdminBootstrap` desde variables de entorno (no por SQL) | No guardar credenciales en el repositorio |

## 4. Decisiones de arquitectura

Ver [ADR](adr/README.md): microservicios, Spring Boot, PostgreSQL por servicio, eventos + outbox, hexagonal, JWT y réplica local.

## 5. Patrones aplicados en el Corte 2

| Patrón | Dónde | Task |
|---|---|---|
| **State** (GoF) | `question-service/domain/state` | A13 |
| **Strategy** (GoF) | `simulation-service/domain/strategy` | B06 |
| **Observer** (GoF) | `swing-client/observer` (vistas suscritas a controladores y sesión) | A20 |
| **Composite** de reglas | `QuestionValidator` + `Rules` | A15 |
| **Flyweight** | `QuestionStates` (estados sin atributos compartidos) | A13 |
| **Proxy** | `TransactionalPorts` (transacciones alrededor de los puertos) | B05, B10 |
| Microkernel + Tuberías y filtros | `QuestionPluginRegistry`, plugins | A14, A21 |
| Hexagonal (puertos y adaptadores) | simulation-service, tracking-service | B05, B10 |
| API Gateway, BD por servicio, Transactional Outbox, consumidor idempotente, DLQ | Infraestructura | A05–A07, A18, B09 |
