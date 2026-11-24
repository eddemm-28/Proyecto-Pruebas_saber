# Architecture Decision Records (ADR) — Corte 2

Formato: Contexto · Decisión · Alternativas · Consecuencias. Estado de todos: **Aceptado** (noviembre 2026).

| ADR | Decisión |
|---|---|
| [ADR-001](ADR-001-microservicios.md) | Pasar del monolito MVC a microservicios por dominio |
| [ADR-002](ADR-002-spring-boot.md) | Spring Boot 3 + Spring Cloud Gateway como plataforma |
| [ADR-003](ADR-003-postgresql-bd-por-servicio.md) | PostgreSQL 16 con una base de datos por servicio y Flyway |
| [ADR-004](ADR-004-eventos-rabbitmq-outbox.md) | Comunicación entre servicios por eventos en RabbitMQ con Transactional Outbox |
| [ADR-005](ADR-005-arquitectura-hexagonal.md) | Arquitectura hexagonal en simulation-service y tracking-service |
| [ADR-006](ADR-006-jwt-gateway.md) | JWT HS256 emitido por user-service y validado en gateway y servicios |
| [ADR-007](ADR-007-replica-local-preguntas.md) | Réplica local de preguntas publicadas en el simulador |
