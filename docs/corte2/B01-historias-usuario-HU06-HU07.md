# B01 — Historias de Usuario HU06 y HU07

> **Corte 2 · Sprint B · Documentación previa** · Responsable: Integrante 3 · Talla M
> Formato tabla para el PDF/SAD. Criterios de aceptación en Dado / Cuando / Entonces.

## HU06 — Presentar simulacros

| Campo | Contenido |
|---|---|
| **ID** | HU06 |
| **Nombre** | Presentar simulacros de la prueba Saber Pro |
| **Como** | Estudiante |
| **Quiero** | Configurar y presentar un simulacro cronometrado con preguntas publicadas del banco |
| **Para** | Practicar en condiciones parecidas a la prueba real y conocer mi resultado de inmediato |
| **Requisitos** | RF-21, RF-22, RF-23, RF-24, RNF-04, RNF-05 |
| **Prioridad / Estimación** | Alta / 13 puntos |
| **Servicio** | `simulation-service` (`svc:simulacros`), hexagonal |
| **Tasks** | B05, B06, B07, B08, B09, B14 |
| **Dependencias** | A18 (eventos de pregunta) y A19 (publicación mínima) |

| # | Criterio de aceptación | Dado | Cuando | Entonces | Prueba |
|---|---|---|---|---|---|
| CA-06.1 | Solo preguntas publicadas | Hay preguntas en Borrador, Aprobadas y Publicadas | Genero un simulacro | Todas sus preguntas están en estado Publicada (vienen de la réplica alimentada por `PreguntaPublicada`) | `SimulationServiceIT`, E2E paso 5 |
| CA-06.2 | Filtro por competencia, tema o dificultad (RF-22) | Elijo la estrategia "Por competencia" y "Lectura crítica" | Inicio | Todas las preguntas son de Lectura crítica y no se repiten | `StrategiesTest` |
| CA-06.3 | Mixto balanceado | Hay 3 competencias con preguntas | Pido 12 preguntas en modo mixto | Recibo 4 de cada competencia, en orden aleatorio | `StrategiesTest.mixtaReparteEntreCompetencias` |
| CA-06.4 | Sin preguntas suficientes | Solo hay 4 preguntas de la competencia | Pido 5 | Veo el mensaje "Solo hay 4 pregunta(s) disponible(s)…" (HTTP 422) | `StrategiesTest.faltanPreguntas` |
| CA-06.5 | Respuesta correcta oculta | Inicio un simulacro | Recibo las preguntas | La respuesta correcta y la justificación no viajan al cliente | `SimulationServiceIT` |
| CA-06.6 | Tiempo controlado en servidor (RF-23) | Mi simulacro venció hace más de 2 s | Envío una respuesta | Se rechaza (HTTP 409), el simulacro queda EXPIRED y se califica con lo ya registrado | `SimulationServiceTest.respuestaFueraDeTiempo…` |
| CA-06.7 | Cierre automático | Cierro la aplicación y el tiempo vence | Pasan ≤ 30 s | El servidor cierra y califica el simulacro sin mi intervención | `SimulationServiceTest.elPlanificadorCierraLosVencidos` |
| CA-06.8 | Cronómetro sincronizado | Mi reloj está 2 min atrasado | Veo el cronómetro | Muestra el tiempo restante según el reloj del servidor | `ClientApiTest.cronometroUsaElRelojDelServidor` |
| CA-06.9 | Un simulacro activo | Tengo un simulacro en curso | Intento iniciar otro | Se me ofrece retomarlo (HTTP 409 con su id) | `SimulationServiceTest.unSoloSimulacroActivo…` |
| CA-06.10 | Calificación por competencia (RF-24) | Respondí 3/3 de Lectura y 0/2 de Razonamiento | Finalizo | Global 60,00; Lectura 100,00; Razonamiento 0,00; las no respondidas cuentan como incorrectas | `SimulationServiceTest.calificaPorCompetencia…` |
| CA-06.11 | Resultado inmediato | Finalizo | — | Veo puntaje global, gráfica por competencia y, por pregunta, mi respuesta, la correcta y la justificación | Vista B14 |
| CA-06.12 | Integración con seguimiento | Finalizo o expira | — | Se emite `SimulacroFinalizado` una sola vez (outbox) | E2E paso 7 |

## HU07 — Seguimiento académico

| Campo | Contenido |
|---|---|
| **ID** | HU07 |
| **Nombre** | Consultar mi seguimiento académico y los reportes de rendimiento |
| **Como** | Estudiante (historial y estadísticas) / Docente o Coordinador (reportes) |
| **Quiero** | Ver mi historial, mis estadísticas por competencia y mis fortalezas y debilidades; y, como docente, reportes agregados |
| **Para** | Enfocar mi preparación en lo que más necesito / orientar el refuerzo del grupo |
| **Requisitos** | RF-25, RF-26, RF-27, RF-28 |
| **Prioridad / Estimación** | Alta / 8 puntos |
| **Servicio** | `tracking-service` (`svc:seguimiento`), hexagonal |
| **Tasks** | B10, B11, B12, B13, B15, B16 |
| **Dependencias** | B09 (`SimulacroFinalizado`) |

| # | Criterio de aceptación | Dado | Cuando | Entonces | Prueba |
|---|---|---|---|---|---|
| CA-07.1 | Historial (RF-25) | Presenté 5 simulacros | Abro "Mi progreso" | Los veo del más reciente al más antiguo, con fecha, tipo, estado, correctas, puntaje y duración, paginados | `TrackingServiceTest.historialPaginado…` |
| CA-07.2 | Estadísticas individuales (RF-26) | Presenté simulacros en varias competencias | Consulto mis estadísticas | Veo promedio global, mejor, último, tendencia y, por competencia, preguntas, aciertos y promedio ponderado | `TrackingServiceTest.promedioPonderado…` |
| CA-07.3 | Fortalezas y debilidades (RF-27) | Mi promedio en una competencia es ≥ 70 % con ≥ 5 preguntas | Consulto | Aparece como **Fortaleza**; si es < 50 % aparece como **Debilidad**; con menos de 5 preguntas queda "En desarrollo" | `TrackingServiceTest.politicaDeClasificacion` |
| CA-07.4 | Consistencia eventual acotada | Acabo de finalizar un simulacro | Abro mi progreso | En ≤ 5 s aparece reflejado | E2E paso 7, escenario QA-3 |
| CA-07.5 | Idempotencia | `SimulacroFinalizado` llega dos veces | — | Se registra una sola vez | `TrackingServiceIT` |
| CA-07.6 | Reporte por competencia (RF-28) | Soy Docente | Genero el reporte en un rango de fechas | Veo por competencia: estudiantes, simulacros, preguntas, aciertos y promedio, de menor a mayor | `TrackingServiceIT` |
| CA-07.7 | Reporte por estudiante | Soy Docente | Abro "Por estudiante" | Veo el ranking y puedo abrir las fortalezas y debilidades de cada uno | Vista B16 |
| CA-07.8 | Temas débiles | Soy Docente | Abro "Temas débiles" | Veo los temas con menor tasa de acierto primero | `TrackingServiceTest.reportesOrdenados` |
| CA-07.9 | Privacidad | Soy Estudiante | Intento ver reportes o datos de otro estudiante | Recibo HTTP 403 | `TrackingServiceIT`, `GatewaySecurityTest` |

## Fuera de alcance (Corte 3)

HU05 Revisión por pares completa (RF-16 a RF-20) y los paneles avanzados de auditoría global de HU08. En el Corte 2 las acciones `start-review`, `approve` y `reject` son el sustituto mínimo (A19).
