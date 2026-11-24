# Documentación del Corte 2 — Banco de Preguntas Saber Pro

Documentación de arquitectura del Corte 2, separada del código. Alimenta el SAD y el PDF final.

## Sprint A — Diseño Ronda 1

| Task | Documento |
|---|---|
| A01 | [Servicios, endpoints REST y eventos RabbitMQ](A01-servicios-endpoints-eventos.md) |
| A02 | [C4 Nivel 1 y 2 + flujo de eventos](A02-diagramas-c4-eventos.md) |
| A03 | [Modelo Entidad-Relación por base de datos](A03-modelo-entidad-relacion.md) |
| A23 | [README raíz: levantar con Docker Compose y ejecutar el cliente](../../README.md) |

## Sprint B — Documentación previa y final (Ronda 2)

| Task | Documento |
|---|---|
| B01 | [Historias de usuario HU06 y HU07](B01-historias-usuario-HU06-HU07.md) |
| B02 | [Wireframes y protocolo Thinking Aloud](B02-wireframes-thinking-aloud.md) |
| B03 | [Escenarios de atributos de calidad](B03-escenarios-calidad.md) |
| B04 | Diagrama de eventos con `SimulacroFinalizado`: [A02 §3](A02-diagramas-c4-eventos.md#3-diagrama-de-flujo-de-eventos) |
| B19 | [C4 Nivel 3, vistas 4+1, ER actualizados y patrones](B19-arquitectura-final.md) · [ADR](adr/README.md) |
| B20 | [Estructura del documento final (PDF/SAD)](B20-estructura-documento-final.md) |
| **SAD** | **Documento final sobre la plantilla SAD:** [PDF](../Documentaci%C3%B3n%20Corte%202.pdf) · [Word](../Documentaci%C3%B3n%20Corte%202.docx) |

## Estructura

```
docs/corte2/
├── A01…A03, B01…B03, B19, B20 (.md)
├── adr/                 ADR-001 … ADR-007
├── diagramas/
│   ├── c4/              Nivel 1, 2 y 3 (Mermaid .mmd)
│   ├── eventos/         flujo-eventos.mmd
│   ├── er/              er-db_*.mmd
│   └── uml/             4+1 en PlantUML (.puml)
└── render/              PNG generados de los .mmd y de los .puml (uml-*.png)
```

## Regenerar imágenes

```bash
npm install -g @mermaid-js/mermaid-cli
cd docs/corte2
for f in $(find diagramas -name '*.mmd'); do mmdc -i "$f" -o "render/$(basename "$f" .mmd).png" -s 2 -b white; done
java -jar plantuml.jar -tpng -o ../../render diagramas/uml/*.puml
```
