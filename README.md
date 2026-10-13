# Sistema para la Gestión de Usuarios y Banco de Preguntas (Saber Pro)

**Universidad del Cauca**  
**Facultad de Ingeniería Electrónica y Telecomunicaciones**  
**Programa de Ingeniería de Sistemas**  
**Laboratorio de Ingeniería de Software II**  
**Taller 4: Patrón Capas y Micro patrón MVC + Observer**  
**Taller 5: Patrones Microkernel y Tuberías y Filtros**  
**Periodo 2-2026**

---

## 📌 Descripción del Proyecto

Este proyecto implementa la aplicación de escritorio monolítica en Java Swing para la gestión de usuarios del sistema de evaluación Saber Pro, cumpliendo con los requerimientos expresados en la guía de laboratorio.

Permite el registro de usuarios con roles específicos, validación rigurosa de complejidad de contraseñas, almacenamiento cifrado seguro con **Argon2id**, persistencia en **SQLite** y un tablero o menú contextual con opciones diferenciadas por el rol del usuario autenticado.

---

## 📐 Cumplimiento de Principios SOLID

El diseño de la arquitectura sigue el ejemplo 5 de Inversión de Dependencias (DIP) visto en la teoría del curso:

1. **Single Responsibility Principle (SRP):**
   - `User`: Representa la entidad de dominio.
   - `PasswordValidator`: Responsable exclusivamente de validar las reglas de complejidad de la contraseña.
   - `PasswordHasher`: Responsable únicamente del hashing y verificación segura utilizando Argon2.
   - `SqliteUserRepository`: Encargado de la persistencia de datos en SQLite.
   - `UserService`: Gestiona la lógica de negocio del registro y autenticación.
   - `LoginFrame`, `RegisterFrame`, `DashboardFrame`: Encargados de la capa de presentación (GUI Swing).

2. **Open/Closed Principle (OCP):**
   - Las abstracciones `IUserRepository`, `IPasswordHasher` y `IPasswordValidator` permiten extender o cambiar la implementación (por ejemplo, cambiar a PostgreSQL, MariaDB o BCrypt) sin modificar el código fuente de `UserService`.

3. **Liskov Substitution Principle (LSP):**
   - `SqliteUserRepository` sustituye limpiamente a `IUserRepository` garantizando el contrato de la interfaz sin comportamiento inesperado.

4. **Interface Segregation Principle (ISP):**
   - Las interfaces son pequeñas y orientadas a propósitos específicos (`IUserRepository`, `IPasswordHasher`, `IPasswordValidator`).

5. **Dependency Inversion Principle (DIP):**
   - El módulo de alto nivel (`UserService`) depende únicamente de interfaces/abstracciones y no de clases concretas de infraestructura. Las dependencias se inyectan a través de su constructor en `Main.java`.

---

## 🔑 Requisitos de Seguridad y Contraseñas

Durante el registro de un usuario, se solicitan los siguientes datos:
- **Nombre de usuario (Login)**
- **Nombre completo**
- **Rol del usuario:** Administrador, Autor de preguntas, Revisor, Docente, Estudiante.
- **Estado del usuario:** Activo / Inactivo.
- **Contraseña:** Debe cumplir con:
  - Mínimo 6 caracteres.
  - Al menos una letra mayúscula.
  - Al menos un dígito.
  - Al menos un carácter especial (ej. `@`, `#`, `$`, `!`, `%`, `*`).
- **Almacenamiento cifrado:** La contraseña nunca se guarda en texto plano; se utiliza **Argon2id** (`argon2-jvm`).

---

## 🛠️ Tecnologías Utilizadas

- **Lenguaje:** Java 17+
- **GUI:** Java Swing
- **Gestor de Dependencias:** Apache Maven
- **Base de Datos:** SQLite (JDBC `sqlite-jdbc`)
- **Seguridad:** Argon2id (`argon2-jvm`)
- **Pruebas Unitarias:** JUnit 5 (Jupiter)

---

## 🚀 Compilación y Ejecución

### 1. Clonar el repositorio
```bash
git clone <URL-del-repositorio>
cd LISWII_T4_G06
```

### 2. Compilar el proyecto
```bash
mvn compile
```

### 3. Ejecutar las Pruebas Unitarias Automatizadas (JUnit 5)
```bash
mvn test
```

### 4. Ejecutar la Aplicación
```bash
mvn exec:java
```

---

## 👥 Usuarios de Prueba Sembrados Inicialmente

Si la base de datos `users.db` está vacía, la aplicación sembrará automáticamente los siguientes usuarios de prueba:

| Login | Contraseña | Rol | Estado |
|---|---|---|---|
| `admin` | `Admin123!` | Administrador | Activo |
| `autor1` | `Autor123!` | Autor de preguntas | Activo |
| `revisor1` | `Revis123!` | Revisor | Activo |
| `docente1` | `Docen123!` | Docente | Activo |
| `estudiante1` | `Estud123!` | Estudiante | Activo |

---

## 📝 Primer Corte: Historias de Usuario Implementadas

| HU | Rol | Qué hace | Clases principales |
|---|---|---|---|
| 1 | Autor de preguntas | Crea preguntas de selección múltiple con contexto, pregunta directa, 4 opciones, respuesta correcta, justificación, bibliografía, competencia, tema, subtema y dificultad. Al grabar se aplica la validación estructural. | `GUIQuestionForm`, `QuestionValidator`, `QuestionService.createQuestion` |
| 2 | Autor de preguntas | Pasa sus preguntas de **Borrador** a **Pendiente de revisión**. Los estados se ven con colores (azul = Borrador, ámbar = Pendiente, rojo = Eliminada). | `GUIMyQuestions`, `EstadoColores`, `QuestionService.sendToReview` |
| 3 | Autor de preguntas | Lista sus preguntas con paginación y filtros (estado, dificultad, tema, competencia y texto). Puede ver o editar sus borradores. | `GUIMyQuestions`, `QuestionFilter`, `PageResult`, `QuestionService.searchByAuthor` |
| 4 | Administrador | Asigna uno o más revisores a las preguntas Pendientes de revisión. Al asignar, cada revisor recibe un correo. | `GUIAssignReviewers`, `ReviewService`, `INotificador` |

### Arquitectura en 3 capas + MVC

- **Presentación (Vista):** `presentation/` (Swing). Solo habla con los controladores.
- **Controlador:** `controller/QuestionController` y `controller/ReviewController`.
- **Dominio (Modelo):** `domain/` con entidades, servicios y abstracciones de repositorio y notificación.
- **Acceso a datos / infraestructura:** `access/` (SQLite) e `infra/` (correo).
- **Composición:** `AppContext` crea los objetos concretos e inyecta las dependencias.

Patrones GoF usados: Observer (las ventanas se refrescan solas), Microkernel, Pipes & Filters y Factory (`NotificadorFactory`).

### Envío de correos (HU4)

1. Copia `mail.properties.example` como `mail.properties` en la carpeta desde donde ejecutas el programa.
2. Completa el servidor SMTP, el usuario y la contraseña (en Gmail, una *contraseña de aplicación*).
3. Si no existe `mail.properties`, los correos se **simulan por consola** y la aplicación funciona igual.

Todo usuario se registra con correo; los revisores reciben el aviso en el que tengan registrado.

### Flujo para probarlo

1. Ingresa con los usuarios de prueba (`autor1`, `revisor1`, `admin`). Sus correos son de ejemplo: si configuraste SMTP y quieres recibir el aviso, registra un revisor nuevo con tu correo real.
2. Como autor: *Crear pregunta* y luego *Mis preguntas* → *Enviar a revisión*.
3. Como administrador: *Asignar revisores* → elige la pregunta y uno o más revisores.

> Las preguntas y las asignaciones se guardan en `users.db` (SQLite). Si ya tenías ese archivo de una versión anterior, la columna `email` se agrega sola.

---

## 🧩 Taller 5: Microkernel + Tuberías y Filtros

### Estructura de paquetes agregada
- `co.edu.unicauca.plugin.api` → contrato `QuestionPlugin` y `QuestionRequest`.
- `co.edu.unicauca.plugin.manager` → `QuestionPluginManager` (carga plugins por reflexión desde `plugins.properties`).
- `co.edu.unicauca.kernel` → `QuestionMicrokernel` (núcleo con `Map<String, Question>`).
- `co.edu.unicauca.pipeline` → `Filter<T>` y `Pipeline<T>` genéricos.
- `co.edu.unicauca.plugins.multiplechoice` → plugin de selección múltiple + los 4 filtros del pipeline.
- `co.edu.unicauca.plugins.casequestion` → plugin de preguntas basadas en casos.
- `co.edu.unicauca.plugins.multimedia` → plugin de preguntas con multimedia.

### Pipeline del plugin de selección múltiple
1. `ContentValidationFilter` — contenido no vacío, longitud mínima.
2. `OptionsValidationFilter` — exactamente 4 opciones, sin vacías, sin duplicadas.
3. `ClassificationFilter` — asigna competencia y dificultad.
4. `CorrectAnswerValidationFilter` — la respuesta correcta coincide con alguna opción.

### Cambio en la vista (Dashboard) por rol
El tablero muestra opciones distintas según el rol autenticado:
- Administrador → Asignar revisores + Banco de preguntas + Generador de plugins.
- Autor de preguntas → Crear pregunta + Mis preguntas + Generador de plugins.
- Revisor → Banco de preguntas.
- Docente → mensaje informativo (reportes/seguimiento pendiente).
- Estudiante → mensaje informativo (simulacros pendiente).

### Ejecución
```bash
mvn exec:java                    # login normal
mvn exec:java -Dexec.args="--plugins"   # abrir directamente el generador de preguntas
```
