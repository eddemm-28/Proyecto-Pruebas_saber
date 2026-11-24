package co.edu.unicauca.saberpro.users.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/** Identidad del usuario autenticado, leída del JWT ya validado. */
public record CurrentUser(UUID id, String login, String role) {

    public static CurrentUser from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("Se esperaba autenticación JWT");
        }
        Jwt jwt = token.getToken();
        return new CurrentUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("login"), jwt.getClaimAsString("role"));
    }
}
