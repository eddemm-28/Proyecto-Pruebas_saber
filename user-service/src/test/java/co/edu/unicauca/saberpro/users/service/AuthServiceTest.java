package co.edu.unicauca.saberpro.users.service;

import co.edu.unicauca.saberpro.users.domain.LoginAudit;
import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserAccount;
import co.edu.unicauca.saberpro.users.domain.UserStatus;
import co.edu.unicauca.saberpro.users.repository.UserRepository;
import co.edu.unicauca.saberpro.users.web.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository users;
    private TokenService tokens;
    private LoginAuditService audit;
    private PasswordEncoder encoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(TokenService.class);
        audit = mock(LoginAuditService.class);
        encoder = new BCryptPasswordEncoder(4); // rápido para pruebas
        service = new AuthService(users, encoder, tokens, audit);
        when(users.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registroCreaEstudianteConHashBCrypt() {
        UserAccount u = service.register("jdiaz", "Juliana Díaz", "jdiaz@unicauca.edu.co", "Segura1*");
        assertEquals(Role.ESTUDIANTE, u.getRole());
        assertEquals(UserStatus.ACTIVO, u.getStatus());
        assertNotEquals("Segura1*", u.getPasswordHash());
        assertTrue(u.getPasswordHash().startsWith("$2"));
        assertTrue(encoder.matches("Segura1*", u.getPasswordHash()));
    }

    @Test
    void registroConLoginDuplicadoDa409() {
        when(users.existsByLoginIgnoreCase("jdiaz")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.register("jdiaz", "Juliana", "j@x.co", "Segura1*"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void registroInvalidoAcumulaErrores() {
        ApiException ex = assertThrows(ApiException.class, () -> service.register("j", "", "no-email", "abc"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertTrue(ex.getErrors().size() >= 4);
        verify(users, never()).save(any());
    }

    @Test
    void loginCorrectoAuditaExitoYEmiteToken() {
        UserAccount u = new UserAccount("autor1", "Autor", "a@x.co", encoder.encode("Segura1*"), Role.AUTOR);
        when(users.findByLoginIgnoreCase("autor1")).thenReturn(Optional.of(u));
        when(tokens.issue(u)).thenReturn(new TokenService.IssuedToken("jwt", 3600, Instant.now()));

        AuthService.LoginResult r = service.login("autor1", "Segura1*", "10.0.0.1", "swing");

        assertEquals("jwt", r.token().value());
        ArgumentCaptor<LoginAudit> captor = ArgumentCaptor.forClass(LoginAudit.class);
        verify(audit).record(captor.capture());
        assertTrue(captor.getValue().isSuccess());
        assertEquals("10.0.0.1", captor.getValue().getIpAddress());
    }

    @Test
    void loginConClaveIncorrectaAuditaFalloY401() {
        UserAccount u = new UserAccount("autor1", "Autor", "a@x.co", encoder.encode("Segura1*"), Role.AUTOR);
        when(users.findByLoginIgnoreCase("autor1")).thenReturn(Optional.of(u));

        ApiException ex = assertThrows(ApiException.class, () -> service.login("autor1", "Mala1*", "ip", "ua"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        ArgumentCaptor<LoginAudit> captor = ArgumentCaptor.forClass(LoginAudit.class);
        verify(audit).record(captor.capture());
        assertFalse(captor.getValue().isSuccess());
        assertEquals(LoginAudit.FailureReason.BAD_CREDENTIALS, captor.getValue().getFailureReason());
    }

    @Test
    void loginDeUsuarioInexistenteAudita() {
        when(users.findByLoginIgnoreCase("nadie")).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.login("nadie", "x", "ip", "ua"));
        ArgumentCaptor<LoginAudit> captor = ArgumentCaptor.forClass(LoginAudit.class);
        verify(audit).record(captor.capture());
        assertEquals(LoginAudit.FailureReason.USER_NOT_FOUND, captor.getValue().getFailureReason());
    }

    @Test
    void loginDeUsuarioInactivoDa403() {
        UserAccount u = new UserAccount("est", "Est", "e@x.co", encoder.encode("Segura1*"), Role.ESTUDIANTE);
        u.changeStatus(UserStatus.INACTIVO);
        when(users.findByLoginIgnoreCase("est")).thenReturn(Optional.of(u));

        ApiException ex = assertThrows(ApiException.class, () -> service.login("est", "Segura1*", "ip", "ua"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(tokens, never()).issue(any());
    }
}
