package co.edu.unicauca.saberpro.users.service;

import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserAccount;
import co.edu.unicauca.saberpro.users.domain.UserAdminAudit;
import co.edu.unicauca.saberpro.users.domain.UserStatus;
import co.edu.unicauca.saberpro.users.repository.UserAdminAuditRepository;
import co.edu.unicauca.saberpro.users.repository.UserRepository;
import co.edu.unicauca.saberpro.users.web.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminServiceTest {

    private UserRepository users;
    private UserAdminAuditRepository audit;
    private UserAdminService service;
    private UserAccount admin;
    private UserAccount estudiante;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        audit = mock(UserAdminAuditRepository.class);
        service = new UserAdminService(users, audit);
        admin = new UserAccount("admin", "Admin", "admin@x.co", "hash", Role.ADMINISTRADOR);
        estudiante = new UserAccount("est", "Est", "est@x.co", "hash", Role.ESTUDIANTE);
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(users.findById(estudiante.getId())).thenReturn(Optional.of(estudiante));
    }

    @Test
    void cambiarRolQuedaAuditado() {
        service.changeRole(estudiante.getId(), Role.AUTOR, admin.getId());

        assertEquals(Role.AUTOR, estudiante.getRole());
        ArgumentCaptor<UserAdminAudit> captor = ArgumentCaptor.forClass(UserAdminAudit.class);
        verify(audit).save(captor.capture());
        assertEquals(UserAdminAudit.Action.ROLE_CHANGED, captor.getValue().getAction());
        assertEquals("ESTUDIANTE", captor.getValue().getOldValue());
        assertEquals("AUTOR", captor.getValue().getNewValue());
    }

    @Test
    void adminNoPuedeInactivarseASiMismo() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.changeStatus(admin.getId(), UserStatus.INACTIVO, admin.getId()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void noSePuedeQuitarElUltimoAdministrador() {
        UUID otroAdmin = UUID.randomUUID();
        when(users.countByRoleAndStatus(Role.ADMINISTRADOR, UserStatus.ACTIVO)).thenReturn(1L);
        assertThrows(ApiException.class, () -> service.changeRole(admin.getId(), Role.DOCENTE, otroAdmin));
        verify(audit, never()).save(any());
    }

    @Test
    void inactivarYReactivarEstudiante() {
        service.changeStatus(estudiante.getId(), UserStatus.INACTIVO, admin.getId());
        assertEquals(UserStatus.INACTIVO, estudiante.getStatus());
        service.changeStatus(estudiante.getId(), UserStatus.ACTIVO, admin.getId());
        assertEquals(UserStatus.ACTIVO, estudiante.getStatus());
    }

    @Test
    void usuarioInexistenteDa404() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.get(id));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
