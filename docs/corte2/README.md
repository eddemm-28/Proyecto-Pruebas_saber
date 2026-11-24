# Documentación del Corte 2 — Banco de Preguntas Saber Pro

Documentación de arquitectura del Corte 2, separada del código. Esta carpeta alimenta el SAD y el PDF final (B19, B20).

## Diseño Ronda 1 (Sprint A)

| Task | Documento | Contenido |
|---|---|---|
| A01 | [A01-servicios-endpoints-eventos.md](A01-servicios-endpoints-eventos.md) | Servicios y límites, seguridad JWT, rutas del gateway, endpoints REST, máquina de estados, topología y contratos de eventos RabbitMQ |
| A02 | [A02-diagramas-c4-eventos.md](A02-diagramas-c4-eventos.md) | C4 Nivel 1 (Contexto), C4 Nivel 2 (Contenedores), flujo de eventos |
| A03 | [A03-modelo-entidad-relacion.md](A03-modelo-entidad-relacion.md) | ER de `db_users`, `db_questions`, `db_simulations`, `db_tracking` |

## Estructura

```
docs/corte2/
├── README.md
├── A01-servicios-endpoints-eventos.md
├── A02-diagramas-c4-eventos.md
├── A03-modelo-entidad-relacion.md
├── diagramas/
│   ├── c4/        c4-nivel1-contexto.mmd, c4-nivel2-contenedores.mmd   (Mermaid)
│   ├── eventos/   flujo-eventos.mmd                                     (Mermaid)
│   ├── er/        er-db_*.mmd                                           (Mermaid erDiagram)
│   └── uml/       (Ronda 2: secuencia, despliegue, clases → PlantUML .puml)
└── render/        PNG generados para el PDF
```

## Regenerar las imágenes

```bash
npm install -g @mermaid-js/mermaid-cli
cd docs/corte2
for f in $(find diagramas -name '*.mmd'); do
  mmdc -i "$f" -o "render/$(basename "$f" .mmd).png" -s 2 -b white
done
```

## Pendiente (Ronda 2)

B04 (eventos, ya incluye `SimulacroFinalizado`), B19 (C4 Nivel 3, 4+1 en PlantUML, ER actualizados, ADRs) y B20 (estructura del PDF).
