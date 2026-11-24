# Banco de Preguntas Saber Pro — Corte 2 (microservicios)

**Universidad del Cauca · FIET · Ingeniería de Sistemas · Ingeniería de Software II · 2026.2**

*Sistema para la Gestión, Validación y Administración de un Banco de Preguntas para la Preparación de las Pruebas Saber Pro.*

El Corte 2 migra el monolito MVC del Corte 1 (conservado en [`legacy/corte1-monolito`](legacy/corte1-monolito)) a una arquitectura de **microservicios Spring Boot** con **PostgreSQL** por servicio, **eventos en RabbitMQ** y un cliente **Java Swing** que consume únicamente el **api-gateway**.

| Integrante | Sprint A | Sprint B |
|---|---|---|
| Integrante 1 — Seguridad y Gateway | A06, A08–A11 | `svc:simulacros` (B05–B09) + B14 |
| Integrante 2 — Banco de preguntas | A12–A19 | `svc:seguimiento` (B10–B13) + B15, B16 |
| Integrante 3 — Infraestructura, broker, Swing y docs | A01–A05, A07, A20–A23 | B01–B04, B17–B20 |

> Completen los nombres de cada integrante (equipo del Corte 1: Eddy Sanchez, Johan Santiago López, Sebastian Tintinago).

---

## Arquitectura

```
 swing-client ──HTTP/JSON + JWT──► api-gateway :8080 ──► user-service :8081 ─────► db_users
                                    (valida JWT, rol)   ─► question-service :8082 ─► db_questions ─┐
                                                         ─► simulation-service :8083 ► db_simulations
                                                         ─► tracking-service :8084 ─► db_tracking
 question-service ──PreguntaPublicada / PreguntaArchivada──► RabbitMQ ──► simulation-service
 simulation-service ──SimulacroFinalizado──────────────────► RabbitMQ ──► tracking-service
```

| Módulo | Qué hace | Estilo |
|---|---|---|
| `api-gateway` | Punto único de entrada, validación JWT, reglas por rol, propagación de `X-User-*` | Spring Cloud Gateway |
| `user-service` | Registro, login (BCrypt + JWT), administración de usuarios, auditoría de logins | Capas |
| `question-service` | Banco de preguntas, **State** (7 estados), validadores RF-08..13, **Microkernel** de plugins, historial, outbox | Capas |
| `simulation-service` | Réplica de preguntas, simulacros con **Strategy**, tiempo en servidor, calificación | **Hexagonal** |
| `tracking-service` | Historial, estadísticas, fortalezas/debilidades, reportes | **Hexagonal** |
| `common-events` | Contratos de eventos e interfaces `EventPublisher` / `EventHandler` | Librería |
| `swing-client` | Cliente de escritorio MVC + **Observer** | Swing |

Documentación de arquitectura (SAD): [`docs/corte2`](docs/corte2/README.md) — servicios y endpoints, C4 1-2-3, ER, eventos, 4+1, ADR, HU, wireframes y escenarios de calidad.

### Alcance

Incluido: HU de usuarios, banco de preguntas, simulacros (HU06) y seguimiento (HU07).
**Excluido para el Corte 3:** HU05 revisión por pares completa (RF-16..20) y la auditoría global avanzada de HU08. En este corte `start-review`, `approve` y `reject` son el sustituto mínimo (A19).

---

## Levantar el sistema (Docker Compose)

**Requisitos:** Docker 24+ con Docker Compose v2, 8 GB de RAM libres. Para el cliente: Java 17+.

```bash
git clone https://github.com/eddemm-28/Proyecto-Pruebas_saber.git
cd Proyecto-Pruebas_saber
cp .env.example .env              # cambie JWT_SECRET y las contraseñas
docker compose up -d --build      # la primera vez compila todo dentro de Docker (5-10 min)
docker compose ps                 # esperar a que todos estén "healthy"
```

| URL | Para qué |
|---|---|
| http://localhost:8080/actuator/health | Salud del gateway |
| http://localhost:15672 | Consola de RabbitMQ (usuario/clave del `.env`) |

Usuario inicial: **`admin` / `Admin123*`** (configurable en `.env`). Las cuentas creadas desde el cliente nacen como **Estudiante**; el administrador asigna los roles Autor, Revisor y Docente.

**Datos de ejemplo** (requiere `curl` y `jq`): crea `autor1`, `revisor1`, `docente1` (clave `Clave123*`) y publica 32 preguntas en 4 competencias:

```bash
./scripts/seed-preguntas.sh 8
```

Para depurar con los puertos de servicios y bases de datos expuestos:
`docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d`

Detener: `docker compose down` (agregue `-v` para borrar los datos).

## Ejecutar el cliente Swing

```bash
mvn -pl swing-client -am package -DskipTests
java -jar swing-client/target/swing-client-2.0.0-SNAPSHOT.jar
# otro servidor:  java -Dsaberpro.api=http://192.168.1.20:8080 -jar swing-client/target/swing-client-2.0.0-SNAPSHOT.jar
```

| Rol | Pestañas |
|---|---|
| Administrador | Banco de preguntas (publicar, archivar), Usuarios y auditoría, Reportes |
| Autor | Mis preguntas (crear manual o con plugin, editar Borradores, enviar a revisión) |
| Revisor | Preguntas por revisar (iniciar revisión, aprobar, rechazar) |
| Docente | Reportes de rendimiento, Banco (lectura) |
| Estudiante | Simulacro (con cronómetro), Mi progreso |

## Desarrollo local sin Docker para los servicios

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db-users db-questions db-simulations db-tracking rabbitmq
mvn -pl user-service -am spring-boot:run          # y así con cada servicio (puertos 8081..8084)
mvn -pl api-gateway spring-boot:run
```
Los `application.yml` ya apuntan a `localhost:5433..5436` y `localhost:5672`. Swagger UI en `http://localhost:808X/swagger-ui.html`.

---

## Pruebas

| Tipo | Comando | Qué cubre |
|---|---|---|
| Unitarias (JUnit 5 + Mockito) | `mvn test` | State, validadores, plugins, permisos, Strategy, calificación, estadísticas, servicios de usuarios y preguntas, cliente Swing contra un gateway simulado |
| Integración (Testcontainers) | `mvn verify` | Cada servicio con PostgreSQL 16 y RabbitMQ reales (`*IT.java`); requiere Docker |
| End-to-end (B17) | `./scripts/e2e-flujo-asincrono.sh` | Publicar pregunta → réplica → simulacro → `SimulacroFinalizado` → estadísticas → reporte → archivar |
| Carga (B18) | `k6 run k6/carga-simulacros.js` | 100 usuarios concurrentes, p95 < 3 s, errores < 1 % (RNF-04, RNF-05) |

k6 con Docker: `docker run --rm -i --network host -v "$PWD/k6:/k6" grafana/k6 run /k6/carga-simulacros.js`
(ejecute antes `./scripts/seed-preguntas.sh 8`).

## Estructura del repositorio

```
├── pom.xml                    # reactor Maven multi-módulo (A04)
├── docker-compose.yml         # A05 (+ docker-compose.dev.yml)
├── docker/service.Dockerfile  # imagen genérica de los servicios
├── common-events/             # A07
├── api-gateway/               # A06
├── user-service/              # A08–A11
├── question-service/          # A12–A19, A21
├── simulation-service/        # B05–B09 (domain / application / infrastructure)
├── tracking-service/          # B10–B13 (domain / application / infrastructure)
├── swing-client/              # A20, B14–B16
├── scripts/                   # semilla de datos y E2E (B17)
├── k6/                        # prueba de carga (B18)
├── docs/corte2/               # SAD del Corte 2
└── legacy/corte1-monolito/    # monolito del Corte 1 (referencia, con la corrección A21)
```

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| El cliente dice "No hay conexión con el servidor" | `docker compose ps`: el gateway debe estar `healthy`; revise `docker compose logs api-gateway` |
| El catálogo del simulacro sale vacío | No hay preguntas **Publicadas**; ejecute `scripts/seed-preguntas.sh` o publique desde el rol Administrador |
| 401 en todas las peticiones | El `JWT_SECRET` cambió tras emitir el token: cierre sesión y vuelva a entrar |
| Un servicio reinicia en bucle | `docker compose logs <servicio>`; suele ser la BD aún no lista o `JWT_SECRET` con menos de 32 caracteres |
