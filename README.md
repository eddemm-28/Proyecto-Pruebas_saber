# Banco de Preguntas Saber Pro

Sistema de escritorio para gestionar el ciclo de vida de preguntas de selección múltiple con única respuesta, orientadas a la preparación de las pruebas Saber Pro del programa de Ingeniería de Sistemas.

Proyecto del curso **Ingeniería de Software II** (Universidad del Cauca, 2026.2). Primer corte: monolito en 3 capas con MVC, principios SOLID y patrones de diseño GoF.

## Equipo

| Integrante | Correo Intitucional |
|---|---|
| Johan Santiago López | <johanl@unicauca.edu.co> |
| Sebastian Tintinago | <sebastiantintinago@unicauca.edu.co> |
| Eddy Sánchez | <eddysanchez@unicauca.edu.co> |

## Alcance del primer corte

| HU | Descripción |
|---|---|
| HU1 | El autor crea preguntas con 10 campos (contexto, pregunta directa, cuatro distractores, respuesta correcta, justificación, bibliografía, competencia, tema, subtema y dificultad) con validación estructural al guardar. |
| HU2 | El autor cambia sus preguntas de «Borrador» a «Pendiente de revisión»; los estados se muestran con colores. |
| HU3 | El autor lista sus preguntas con paginación y filtros. |
| HU4 | El administrador asigna revisores a preguntas «Pendientes de revisión» y el sistema envía una notificación por correo. |

## Tecnologías

- Java 17
- Maven
- Java Swing (interfaz gráfica)
- SQLite (`sqlite-jdbc`)
- Argon2 (`argon2-jvm`) para el hash de contraseñas
- Jakarta Mail para las notificaciones
- JUnit 5 para las pruebas unitarias

## Arquitectura

Monolito de escritorio en capas, con MVC en la capa de presentación:

```
src/main/java/co/edu/unicauca/
├── presentation/   Interfaz Swing (vistas)
├── controller/     Controladores que median entre vistas y dominio
├── domain/         Entidades, servicios y validadores
├── access/         Repositorios y acceso a SQLite
├── infra/          Notificaciones (correo y consola)
├── kernel/         Microkernel para tipos de pregunta
├── pipeline/       Tuberías y filtros
└── plugin(s)/      Plugins por tipo de pregunta
```

### Patrones de diseño

- **MVC**: separación de vistas, controladores y modelo.
- **Repository**: el dominio depende de interfaces; SQLite es un detalle de acceso a datos.
- **Observer**: las vistas reaccionan a los cambios en las preguntas.
- **Factory**: `NotificadorFactory` crea el notificador (correo o consola).
- **Microkernel** y **Pipes and Filters**: extensión por plugins para nuevos tipos de pregunta y sus filtros de validación.

## Estructura del repositorio

```
.
├── docs/                      Documento de arquitectura y diagramas (C4, UML, ER)
├── src/main/java              Código fuente
├── src/main/resources         Configuración (plugins)
├── src/test/java              Pruebas unitarias
├── mail.properties.example    Plantilla de configuración del correo
└── pom.xml
```

## Ejecución

Requisitos: JDK 17 y Maven.

```bash
mvn compile
mvn exec:java        # clase principal: co.edu.unicauca.Main
mvn test             # pruebas unitarias
```

### Notificaciones por correo

1. Copiar `mail.properties.example` a `mail.properties`.
2. Completar las credenciales SMTP.

## Flujo de trabajo

- Metodología Scrum; tablero del sprint en Trello
- Mensajes de commit con Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).

## Entregables

- Documento de arquitectura: _enlace pendiente_
- Video de sustentación (YouTube): _enlace pendiente_