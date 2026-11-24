# ADR-004 — Eventos de dominio en RabbitMQ con Transactional Outbox

**Contexto.** Publicar una pregunta debe habilitarla en el simulador, y finalizar un simulacro debe actualizar el seguimiento, sin acoplar los servicios ni perder información si un componente cae (RNF-17).

**Decisión.** Exchange topic `saberpro.events` con routing keys `question.published`, `question.archived` y `simulation.finished`; cada consumidor declara su cola durable con DLQ. Los productores escriben el evento en `outbox_events` en la misma transacción del cambio de negocio y un *relay* lo publica con *publisher confirms*. Los consumidores son idempotentes (`processed_events`). Interfaces `EventPublisher` / `EventHandler` en `common-events` (A07).

**Alternativas.** REST síncrono entre servicios (acoplamiento temporal, fallas en cascada); publicar directo a RabbitMQ tras el commit (se pierden eventos si el broker cae entre el commit y el envío); Kafka (más pesado para el alcance del curso).

**Consecuencias.** + Sin pérdida de eventos, servicios desacoplados, nuevos consumidores sin tocar al productor. − Consistencia eventual (≤ 5 s, escenario QA-3), entrega *at-least-once* que exige idempotencia, una tabla y un proceso programado más por productor.
