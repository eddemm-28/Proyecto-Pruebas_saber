# B03 — Escenarios de atributos de calidad

> **Corte 2 · Sprint B · Documentación previa** · Responsable: Integrante 3 · Talla S
> Formato de seis partes (Bass, Clements, Kazman): Fuente · Estímulo · Artefacto · Entorno · Respuesta · Medida.
> RNF-04 y RNF-05 se verifican con `k6/carga-simulacros.js` (B18); RNF-17 con la E2E y pruebas de tolerancia a fallos.

## QA-1 — Desempeño: tiempo de respuesta (RNF-04)

| Parte | Descripción |
|---|---|
| **Fuente** | Estudiante usando el cliente Swing |
| **Estímulo** | Inicia un simulacro, responde preguntas y lo finaliza |
| **Artefacto** | `api-gateway` + `simulation-service` + `db_simulations` |
| **Entorno** | Operación normal con 100 usuarios concurrentes, despliegue Docker Compose en un equipo de 8 GB RAM |
| **Respuesta** | Cada operación se atiende y se confirma al cliente |
| **Medida** | p95 de `http_req_duration` **< 3 s** en todo el flujo; p95 de "responder" < 1 s |

**Tácticas:** réplica local de preguntas (sin llamada REST al question-service por simulacro), índices `ix_replica_available` y `ix_sim_student_status`, pool Hikari de 20 conexiones, `open-in-view=false`, publicación de eventos fuera del camino de la petición (outbox con relay asíncrono).

## QA-2 — Escalabilidad / concurrencia (RNF-05)

| Parte | Descripción |
|---|---|
| **Fuente** | 100 estudiantes en simultáneo (jornada de práctica) |
| **Estímulo** | Rampa de 0 a 100 usuarios en 30 s y 2 min sostenidos presentando simulacros |
| **Artefacto** | Sistema completo |
| **Entorno** | Carga pico |
| **Respuesta** | El sistema procesa todos los simulacros sin perder respuestas ni mezclar sesiones |
| **Medida** | Tasa de error HTTP **< 1 %**, flujo completo correcto **> 99 %**, ningún estudiante con dos simulacros activos |

**Tácticas:** servicios sin estado (JWT, sin sesión de servidor), preparados para escalar horizontalmente (en Corte 3 se puede añadir balanceo con varias réplicas detrás del gateway); índice único parcial `uk_sim_one_active`; bloqueo optimista (`@Version`) en simulacros y preguntas.

## QA-3 — Disponibilidad y consistencia del flujo asíncrono (RNF-17)

| Parte | Descripción |
|---|---|
| **Fuente** | Falla de infraestructura |
| **Estímulo** | RabbitMQ o el `tracking-service` se detienen mientras los estudiantes finalizan simulacros |
| **Artefacto** | Outbox del `simulation-service`, colas durables, `tracking-service` |
| **Entorno** | Operación degradada |
| **Respuesta** | Los estudiantes siguen presentando y finalizando simulacros; al recuperarse el componente, los resultados llegan al seguimiento sin pérdidas ni duplicados |
| **Medida** | **0 eventos perdidos** y **0 duplicados**; seguimiento actualizado ≤ **5 s** después de la recuperación (≤ 5 s en operación normal) |

**Tácticas:** Transactional Outbox, *publisher confirms*, colas y mensajes persistentes, reintentos (3, backoff exponencial) y DLQ, idempotencia por `eventId` (`processed_events`) y por `simulation_id`, versión de origen para descartar eventos viejos.

**Procedimiento de verificación:**
1. `docker compose stop rabbitmq` → presentar y finalizar 3 simulacros (responden 200).
2. Verificar en `db_simulations.outbox_events` 3 filas con `published_at IS NULL`.
3. `docker compose start rabbitmq` → en ≤ 5 s las 3 filas quedan publicadas y `GET /api/tracking/students/me/history` muestra los 3.
4. Repetir con `docker compose stop tracking-service`: los mensajes esperan en `tracking.simulation-finished.q` (UI :15672).

## QA-4 — Seguridad (RNF-06, RNF-07, RNF-08)

| Parte | Descripción |
|---|---|
| **Fuente** | Usuario malicioso con una cuenta de Estudiante |
| **Estímulo** | Llama directamente a `/api/questions`, `/api/tracking/reports/**` o falsifica la cabecera `X-User-Role: ADMINISTRADOR` |
| **Artefacto** | `api-gateway` y resource servers de cada servicio |
| **Entorno** | Operación normal |
| **Respuesta** | La petición se rechaza; la cabecera falsificada se descarta |
| **Medida** | 100 % de intentos rechazados con 401/403 (`GatewaySecurityTest`, E2E paso 2); contraseñas solo como hash BCrypt |

## QA-5 — Modificabilidad (Corte 3)

| Parte | Descripción |
|---|---|
| **Fuente** | Equipo de desarrollo |
| **Estímulo** | Incorporar HU05 (revisión por pares) y un nuevo consumidor de eventos (notificaciones) |
| **Artefacto** | `question-service` (State en `EN_REVISION`), `common-events` |
| **Entorno** | Tiempo de desarrollo |
| **Respuesta** | Se agregan sin modificar `simulation-service` ni `tracking-service` |
| **Medida** | 0 líneas cambiadas en los servicios hexagonales; nuevo consumidor = nueva cola ligada al exchange existente |
