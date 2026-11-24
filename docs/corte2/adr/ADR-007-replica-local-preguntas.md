# ADR-007 — Réplica local de preguntas publicadas en el simulador

**Contexto.** Generar un simulacro necesita consultar muchas preguntas publicadas. Consultar al `question-service` en cada simulacro acopla la disponibilidad de ambos servicios y suma latencia con 100 usuarios (RNF-04/05).

**Decisión.** El `simulation-service` mantiene `question_replica` alimentada por `PreguntaPublicada` y `PreguntaArchivada`. Al generar un simulacro, cada ítem guarda una "foto" de competencia, tema, dificultad y respuesta correcta.

**Alternativas.** Llamada REST síncrona con caché (más simple, pero acoplamiento temporal); base compartida (prohibida por el DoD).

**Consecuencias.** + El simulador funciona aunque el `question-service` esté caído; los simulacros siguen siendo calificables si la pregunta se archiva después. − Datos duplicados y consistencia eventual (una pregunta publicada tarda segundos en aparecer en el catálogo).
