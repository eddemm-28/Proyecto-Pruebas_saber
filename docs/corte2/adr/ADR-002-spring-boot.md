# ADR-002 — Spring Boot 3 y Spring Cloud Gateway

**Contexto.** El equipo domina Java 17. Se necesitan REST, JPA, seguridad JWT, AMQP, migraciones y un gateway.

**Decisión.** Spring Boot 3.3 (Web, Data JPA, Security/OAuth2 Resource Server, AMQP, Actuator, Validation) y Spring Cloud Gateway 2023.0 para el gateway. Reactor multi-módulo Maven (A04) con `common-events` como librería compartida de contratos.

**Alternativas.** Quarkus o Micronaut (arranque más rápido, menor experiencia del equipo); Javalin/Spark (más código de plomería); gateway con Nginx (no valida JWT con roles sin módulos extra).

**Consecuencias.** + Ecosistema integrado, Testcontainers con `@ServiceConnection`, mucha documentación. − Mayor consumo de memoria por contenedor (~250–350 MB); se mitiga con `-XX:MaxRAMPercentage=75` y JRE Alpine.
