# ADR-001 — Microservicios por dominio

**Contexto.** El Corte 1 es un monolito Swing + SQLite en capas. El Corte 2 agrega simulacros y seguimiento con 100 usuarios concurrentes (RNF-05), y el Corte 3 agregará la revisión por pares. El curso exige una arquitectura distribuida y tres integrantes trabajan en paralelo.

**Decisión.** Cuatro servicios alineados con los dominios (`svc:usuarios`, `svc:preguntas`, `svc:simulacros`, `svc:seguimiento`) más un `api-gateway`. Cada servicio es dueño de sus datos y se despliega en su propio contenedor.

**Alternativas.** (a) Monolito modular con Spring Boot: más simple de operar, pero no cumple el objetivo académico ni permite escalar el simulador por separado. (b) Más servicios (p. ej. uno de notificaciones o de reportes): más costo operativo sin necesidad en este corte.

**Consecuencias.** + Despliegue y escalado independientes, límites claros para dividir el trabajo en PRs. − Consistencia eventual entre servicios, más infraestructura (5 contenedores de aplicación, 4 BD, broker) y pruebas E2E necesarias (B17).
