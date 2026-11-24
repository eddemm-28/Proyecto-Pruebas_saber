# ADR-005 — Arquitectura hexagonal en simulation-service y tracking-service

**Contexto.** Simulacros y seguimiento son servicios nuevos con la lógica de negocio más rica del Corte 2 (Strategy, tiempo en servidor, calificación, estadísticas). En el Corte 3 se agregarán patrones GoF y posiblemente otros adaptadores (notificaciones, exportación).

**Decisión.** Paquetes `domain` (modelo y reglas, Java puro), `application` (puertos de entrada/salida y casos de uso, Java puro) e `infrastructure` (adaptadores REST, AMQP, JPA, scheduler y configuración). Las transacciones se aplican envolviendo los puertos de entrada con un proxy (`TransactionalPorts`), sin anotaciones de Spring en el núcleo.

**Alternativas.** Capas clásicas con entidades JPA en el dominio (más rápido de escribir, pero acopla las reglas a Hibernate y obliga a probar con base de datos).

**Consecuencias.** + El núcleo se prueba con dobles en memoria y sin Spring (28 pruebas que corren en milisegundos), se pueden cambiar adaptadores sin tocar reglas. − Mapeo dominio↔JPA explícito y más clases; `question-service` y `user-service` conservan capas porque vienen del Corte 1.
