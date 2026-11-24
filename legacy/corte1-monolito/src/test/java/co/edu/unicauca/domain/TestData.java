package co.edu.unicauca.domain;

/**
 * Datos de prueba reutilizables y dobles de prueba para los tests.
 */
class TestData {

    static Question preguntaValida(String autor) {
        return new Question(
                null,
                "Pregunta de prueba",
                "Una empresa necesita que su sistema sea fácil de modificar.",
                "¿Qué principio de diseño ayuda más a lograrlo?",
                new QuestionDistractors("Abierto/Cerrado", "Copiar y pegar", "Acoplamiento fuerte", "Variables globales"),
                "A",
                "El principio Abierto/Cerrado permite extender sin modificar código existente.",
                "Martin, R. Clean Architecture. 2017.",
                "Diseño de software",
                "Principios SOLID",
                "OCP",
                "Media",
                Question.STATE_BORRADOR,
                autor);
    }

    static User usuario(String login, Role role, UserStatus status, String email) {
        return new User(login, "Nombre " + login, role, status, "hash", email);
    }
}
