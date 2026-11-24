# B20 — Estructura del documento final (PDF) según la plantilla SAD

> **Corte 2 · Sprint B · Documentación final** · Responsable: Integrante 3 · Talla S
> Cada sección indica de qué archivo del repositorio sale su contenido.

| # | Sección del PDF | Contenido | Fuente |
|---|---|---|---|
| 0 | **Portada** | Universidad del Cauca, FIET, Programa de Ingeniería de Sistemas; Ingeniería de Software II 2026.2; título del proyecto; "Segundo corte"; integrantes; docente; fecha | — |
| 1 | **Resumen** | ½ página: migración del monolito MVC a 4 microservicios + gateway con eventos RabbitMQ; HU06 y HU07 nuevas; HU05 para Corte 3; resultados de pruebas y carga | README raíz |
| 2 | **Introducción y alcance** | Objetivo del corte, HU incluidas y excluidas, supuestos | [A01 §1](A01-servicios-endpoints-eventos.md) |
| 3 | **Historias de usuario** | HU06 y HU07 con criterios de aceptación; resumen de HU del Sprint A | [B01](B01-historias-usuario-HU06-HU07.md) |
| 4 | **Wireframes y Thinking Aloud** | W1–W6, protocolo, tabla de resultados, cambios aplicados, capturas reales del cliente | [B02](B02-wireframes-thinking-aloud.md) |
| 5 | **Escenarios de calidad** | QA-1 a QA-5 y evidencia (salida de k6, prueba de caída de RabbitMQ) | [B03](B03-escenarios-calidad.md), `k6/results/resumen.json` |
| 6 | **Vista de contexto y contenedores** | C4 Nivel 1 y 2, tabla de servicios | [A02](A02-diagramas-c4-eventos.md), [A01 §2](A01-servicios-endpoints-eventos.md) |
| 7 | **Vista de componentes** | C4 Nivel 3 de los tres servicios principales | [B19 §1](B19-arquitectura-final.md) |
| 8 | **Vistas 4+1** | Lógica (State, estados), procesos (secuencia), desarrollo (hexágono), física (despliegue) | [B19 §2](B19-arquitectura-final.md) |
| 9 | **Comunicación** | Endpoints REST, catálogo de eventos, topología RabbitMQ, garantías de entrega, diagrama de eventos | [A01 §5–6](A01-servicios-endpoints-eventos.md), [A02 §3](A02-diagramas-c4-eventos.md) |
| 10 | **Modelo de datos** | ER por base de datos y cambios respecto al Corte 1 | [A03](A03-modelo-entidad-relacion.md), [B19 §3](B19-arquitectura-final.md) |
| 11 | **Decisiones de arquitectura** | ADR-001 a ADR-007 | [adr/](adr/README.md) |
| 12 | **Patrones de diseño** | Tabla de patrones y fragmentos de código representativos | [B19 §5](B19-arquitectura-final.md) |
| 13 | **Pruebas** | Unitarias, integración (Testcontainers), E2E, carga: cómo se ejecutan y resultados | README raíz §Pruebas |
| 14 | **Despliegue** | Requisitos y pasos con Docker Compose; ejecución del cliente | README raíz |
| 15 | **Trabajo para el Corte 3** | HU05 completa, auditoría global HU08, RS256/JWKS, patrones GoF adicionales | [A01 §1](A01-servicios-endpoints-eventos.md) |
| 16 | **Repositorio** | URL de GitHub: `https://github.com/eddemm-28/Proyecto-Pruebas_saber`, rama y etiqueta del corte (`v2.0-corte2`) | — |
| A | **Anexos** | Swagger de cada servicio (`/swagger-ui.html`), salida completa de pruebas, consentimiento de usabilidad | — |

**Recomendaciones de formato:** diagramas exportados en PNG a 2× desde `render/`; tablas largas (endpoints) en horizontal; numerar figuras y tablas; citar ICFES y Bass/Clements/Kazman.
