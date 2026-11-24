# ADR-003 — PostgreSQL 16, una base por servicio, Flyway

**Contexto.** SQLite (Corte 1) no soporta bien escritura concurrente ni red. El DoD exige BD aislada por servicio.

**Decisión.** Un contenedor PostgreSQL 16 por servicio (`db_users`, `db_questions`, `db_simulations`, `db_tracking`) con credenciales propias. Esquema versionado con Flyway (`V1__init.sql`) y Hibernate en `ddl-auto=validate`. Referencias entre servicios como UUID sin FK física.

**Alternativas.** Una instancia con varios esquemas (menos recursos pero tentación de hacer JOIN entre servicios); `ddl-auto=update` (esquema no reproducible ni revisable).

**Consecuencias.** + Aislamiento real, índices parciales (`uk_sim_one_active`), JSONB para el outbox, migraciones revisables en PR. − Cuatro contenedores de BD; los datos de otros servicios se copian (p. ej. `author_login`, `student_login`).
