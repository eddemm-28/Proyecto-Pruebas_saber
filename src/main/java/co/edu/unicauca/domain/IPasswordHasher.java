package co.edu.unicauca.domain;

/**
 * Interfaz para la abstracción del cifrado/hashing de contraseñas (DIP).
 */
public interface IPasswordHasher {
    String hash(String password);
    boolean verify(String hash, String password);
}

