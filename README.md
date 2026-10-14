# Banco de Preguntas Saber Pro

**Universidad del Cauca**
**Facultad de Ingeniería Electrónica y Telecomunicaciones**
**Programa de Ingeniería de Sistemas**
**Ingeniería de Software II (Teoría y Laboratorio) - Periodo 2026.2**
**Proyecto de curso - Primer corte**

---

## Descripción

Aplicación de escritorio monolítica en Java Swing para gestionar el ciclo de vida de preguntas de selección múltiple con única respuesta, orientadas a la preparación de las pruebas Saber Pro del programa de Ingeniería de Sistemas.

Incluye gestión de usuarios con roles, creación y validación de preguntas, listado con paginación y filtros, y asignación de revisores con notificación por correo. La persistencia es SQLite y las contraseñas se almacenan con **Argon2id**.

El código parte de los talleres 4 (capas, MVC y Observer) y 5 (Microkernel y Tuberías y Filtros) del laboratorio y se extiende con las historias de usuario del primer corte.

## Equipo

| Integrante | Responsabilidades principales |
|---|---|
| Eddy Sanchez | Programador y Analista |
| Johan Santiago López | Analista y Testing |
| Sebastian Tintinago| Programador y Testing |

---

## Primer corte: historias de usuario implementadas

| HU | Rol | Qué hace | Clases principales |
|---|---|---|---|
| 1 | Autor de preguntas | Crea preguntas de selección múltiple con contexto, pregunta directa, 4 opciones, respuesta correcta, justificación, bibliografía, competencia, tema, subtema y dificultad. Al grabar se aplica la validación estructural. | `GUIQuestionForm`, `QuestionValidator`, `QuestionService.createQuestion` |
| 2 | Autor de preguntas | Pasa sus preguntas de **Borrador** a **Pendiente de revisión**. Los estados se ven con colores (azul = Borrador, ámbar = Pendiente, rojo = Eliminada). | `GUIMyQuestions`, `EstadoColores`, `QuestionService.sendToReview` |
| 3 | Autor de preguntas | Lista sus preguntas con paginación y filtros (estado, dificultad, tema, competencia y texto). Puede ver o editar sus borradores. | `GUIMyQuestions`, `QuestionFilter`, `PageResult`, `QuestionService.searchByAuthor` |
| 4 | Administrador | Asigna uno o más revisores a las preguntas Pendientes de revisión. Al asignar, cada revisor recibe un correo. | `GUIAssignReviewers`, `ReviewService`, `INotificador` |

### Validación estructural al grabar (HU1)

`QuestionValidator` verifica: campos obligatorios completos, longitud mínima de los textos largos, cuatro opciones no vacías y no repetidas, y respuesta correcta entre A y D.

---

## Arquitectura

Monolito de escritorio en 3 capas, con MVC en la capa de presentación.

```
src/main/java/co/edu/unicauca/
├── presentation/   Vista (Swing). Solo habla con los controladores
├── controller/     QuestionController, ReviewController
├── domain/         Entidades, servicios, validadores y abstracciones
├── access/         Repositorios SQLite
├── infra/          Notificaciones (correo y consola)
├── kernel/         QuestionMicrokernel
├── pipeline/       Filter<T> y Pipeline<T>
├── plugin/         API y gestor de plugins
└── plugins/        multiplechoice, casequestion, multimedia
```

- **Presentación (Vista):** `presentation/`.
- **Controlador:** `controller/`.
- **Dominio (Modelo):** `domain/`, con entidades, servicios y abstracciones de repositorio y notificación.
- **Acceso a datos e infraestructura:** `access/` (SQLite) e `infra/` (correo).
- **Composición:** `AppContext` crea los objetos concretos e inyecta las dependencias por constructor.

### Patrones

Patrones GoF:
- **Observer:** las ventanas se refrescan cuando cambian las preguntas (`Subject`/`Observer`).
- **Factory:** `NotificadorFactory` crea el notificador de correo o de consola.

Patrones de acceso a datos y arquitectónicos:
- **Repository:** el dominio depende de interfaces; SQLite es un detalle de `access/`.
- **Capas y MVC:** separación de presentación, controlador, dominio y datos.
- **Microkernel y Pipes and Filters:** módulo de plugins para nuevos tipos de pregunta (ver Taller 5).

### Principios SOLID

1. **SRP:** cada clase tiene una responsabilidad. `User` y `Question` son entidades; `QuestionValidator` valida; `PasswordHasher` aplica Argon2; los repositorios `Sqlite*` persisten; `UserService`, `QuestionService` y `ReviewService` contienen la lógica de negocio; las clases `GUI*` y `*Frame` son la presentación.
2. **OCP:** las abstracciones (`IUserRepository`, `QuestionRepository`, `ReviewAssignmentRepository`, `INotificador`, `IPasswordHasher`, `IPasswordValidator`) permiten cambiar la base de datos, el hash o el canal de notificación sin tocar los servicios. Los nuevos tipos de pregunta se agregan como plugins sin modificar el núcleo.
3. **LSP:** las implementaciones (`Sqlite*Repository`, notificadores de correo y consola) sustituyen a sus interfaces sin cambiar el comportamiento esperado.
4. **ISP:** interfaces pequeñas y específicas.
5. **DIP:** `UserService`, `QuestionService` y `ReviewService` dependen de interfaces, no de clases concretas; las dependencias se inyectan en `AppContext`.

---

## Requisitos de seguridad y contraseñas

Durante el registro se solicitan:
- **Nombre de usuario (login).**
- **Nombre completo y correo electrónico.**
- **Rol:** Administrador, Autor de preguntas, Revisor, Docente, Estudiante.
- **Estado:** Activo / Inactivo.
- **Contraseña:** mínimo 6 caracteres, al menos una mayúscula, un dígito y un carácter especial (`@`, `#`, `$`, `!`, `%`, `*`).

La contraseña nunca se guarda en texto plano: se utiliza **Argon2id** (`argon2-jvm`).

---

## Tecnologías

- **Lenguaje:** Java 17+
- **GUI:** Java Swing
- **Gestor de dependencias:** Apache Maven
- **Base de datos:** SQLite (`sqlite-jdbc`)
- **Seguridad:** Argon2id (`argon2-jvm`)
- **Correo:** Jakarta Mail
- **Pruebas unitarias:** JUnit 5 (Jupiter)

---

## Compilación y ejecución

### 1. Clonar el repositorio
```bash
git clone <URL-del-repositorio>
cd <nombre-del-repositorio>
```

### 2. Compilar
```bash
mvn compile
```

### 3. Pruebas unitarias (JUnit 5)
```bash
mvn test
```
Cubren entidades, servicios y validadores del dominio, repositorios y controladores.

### 4. Ejecutar
```bash
mvn exec:java                             # inicio de sesión normal
mvn exec:java -Dexec.args="--questions"   # abrir directamente el módulo de preguntas
mvn exec:java -Dexec.args="--plugins"     # abrir directamente el generador de preguntas
```

---

## Usuarios de prueba sembrados

Si la base de datos `users.db` está vacía, la aplicación siembra estos usuarios:

| Login | Contraseña | Rol | Estado |
|---|---|---|---|
| `admin` | `Admin123!` | Administrador | Activo |
| `autor1` | `Autor123!` | Autor de preguntas | Activo |
| `revisor1` | `Revis123!` | Revisor | Activo |
| `docente1` | `Docen123!` | Docente | Activo |
| `estudiante1` | `Estud123!` | Estudiante | Activo |

Las preguntas y las asignaciones se guardan en `users.db` (SQLite). Si ya existía ese archivo de una versión anterior, la columna `email` se agrega sola.

### Flujo para probarlo

1. Ingresa con `autor1`, `revisor1` y `admin`. Sus correos son de ejemplo; para recibir el aviso real, registra un revisor con tu correo.
2. Como autor: *Crear pregunta* y luego *Mis preguntas* → *Enviar a revisión*.
3. Como administrador: *Asignar revisores* → elige la pregunta y uno o más revisores.

### Envío de correos (HU4)

1. Copia `mail.properties.example` como `mail.properties` en la carpeta desde donde ejecutas el programa.
2. Completa el servidor SMTP, el usuario y la contraseña (en Gmail, una *contraseña de aplicación*).
3. Si no existe `mail.properties`, los correos se **simulan por consola** y la aplicación funciona igual.

`mail.properties` y `users.db` están en `.gitignore` y no deben subirse.

---

## Taller 5: Microkernel + Tuberías y Filtros

### Paquetes
- `plugin.api`: contrato `QuestionPlugin` y `QuestionRequest`.
- `plugin.manager`: `QuestionPluginManager` (carga plugins por reflexión desde `plugins.properties`).
- `kernel`: `QuestionMicrokernel` (núcleo con `Map<String, Question>`).
- `pipeline`: `Filter<T>` y `Pipeline<T>` genéricos.
- `plugins.multiplechoice`: plugin de selección múltiple y sus 4 filtros.
- `plugins.casequestion`: plugin de preguntas basadas en casos.
- `plugins.multimedia`: plugin de preguntas con multimedia.

### Pipeline del plugin de selección múltiple
1. `ContentValidationFilter`: contenido no vacío, longitud mínima.
2. `OptionsValidationFilter`: exactamente 4 opciones, sin vacías, sin duplicadas.
3. `ClassificationFilter`: asigna competencia y dificultad.
4. `CorrectAnswerValidationFilter`: la respuesta correcta coincide con alguna opción.

### Vista por rol (Dashboard)
- Administrador: Asignar revisores, Banco de preguntas y Generador de plugins.
- Autor de preguntas: Crear pregunta, Mis preguntas y Generador de plugins.
- Revisor: Banco de preguntas.
- Docente: mensaje informativo (reportes y seguimiento pendientes).
- Estudiante: mensaje informativo (simulacros pendientes).

---

## Estructura del repositorio

```
.
├── docs/                      Documento de arquitectura y diagramas (C4, UML, ER)
├── src/main/java             Código fuente
├── src/main/resources        Configuración (plugins.properties)
├── src/test/java             Pruebas unitarias
├── mail.properties.example   Plantilla de configuración del correo
└── pom.xml
```

## Flujo de trabajo

- Metodología Scrum; tablero del Sprint 1 en Trello: [Trello-Soft_II](https://trello.com/invite/b/6acdb7e2de7a7de28b4deabe/ATTI96ee1d851b33da793e26f1665193978d6D5C0891/soft-ii-sistema-pruebas-saber).
- Mensajes de commit con Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).

## Entregables

- Documento de arquitectura: [Enlace a Drive](https://drive.google.com/drive/folders/1NiCGFvfRr0prJAtw_y8NcZm89vNoj_wU?usp=sharing)
- Video de sustentación (YouTube): _enlace pendiente_