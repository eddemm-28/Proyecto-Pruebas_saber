package co.edu.unicauca.saberpro.users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret (JWT_SECRET) debe tener al menos 32 bytes para HS256");
        }
        if (expirationMinutes <= 0) {
            expirationMinutes = 60;
        }
    }
}
