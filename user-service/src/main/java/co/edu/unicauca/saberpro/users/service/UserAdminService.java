package co.edu.unicauca.saberpro.users.service;

import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserAccount;
import co.edu.unicauca.saberpro.users.domain.UserAdminAudit;
import co.edu.unicauca.saberpro.users.domain.UserStatus;
import co.edu.unicauca.saberpro.users.repository.UserAdminAuditRepository;
import co.edu.unicauca.saberpro.users.repository.UserRepository;
import co.edu.unicauca.saberpro.users.web.error.ApiException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A09 / RF-03: listar, activar/inactivar y cambiar el rol de los usuarios. */
@Service
public class UserAdminService {

    private final UserRepository users;
    private final UserAdminAuditRepository adminAudit;

    public UserAdminService(UserRepository users, UserAdminAuditRepository adminAudit) {
        this.users = users;
        this.adminAudit = adminAudit;
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> search(Role role, UserStatus status, String q, Pageable pageable) {
        Specification<UserAccount> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (role != null) p.add(cb.equal(root.get("role"), role));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase().trim() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("login")), like),
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return users.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public UserAccount get(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado: " + id));
    }

    @Transactional
    public UserAccount changeStatus(UUID targetId, UserStatus newStatus, UUID actorId) {
        UserAccount target = get(targetId);
        if (target.getStatus() == newStatus) {
            return target;
        }
        if (targetId.equals(actorId) && newStatus == UserStatus.INACTIVO) {
            throw ApiException.conflict("Un administrador no puede inactivarse a sí mismo.");
        }
        if (newStatus == UserStatus.INACTIVO) {
            ensureNotLastActiveAdmin(target);
        }
        String old = target.getStatus().name();
        target.changeStatus(newStatus);
        adminAudit.save(new UserAdminAudit(targetId, actorId,
                newStatus == UserStatus.ACTIVO ? UserAdminAudit.Action.ACTIVATED : UserAdminAudit.Action.INACTIVATED,
                old, newStatus.name()));
        return target;
    }

    @Transactional
    public UserAccount changeRole(UUID targetId, Role newRole, UUID actorId) {
        UserAccount target = get(targetId);
        if (target.getRole() == newRole) {
            return target;
        }
        if (target.getRole() == Role.ADMINISTRADOR) {
            if (targetId.equals(actorId)) {
                throw ApiException.conflict("Un administrador no puede quitarse a sí mismo el rol de administrador.");
            }
            ensureNotLastActiveAdmin(target);
        }
        String old = target.getRole().name();
        target.changeRole(newRole);
        adminAudit.save(new UserAdminAudit(targetId, actorId, UserAdminAudit.Action.ROLE_CHANGED, old, newRole.name()));
        return target;
    }

    @Transactional(readOnly = true)
    public List<UserAdminAudit> history(UUID targetId) {
        get(targetId);
        return adminAudit.findByTargetUserIdOrderByOccurredAtDesc(targetId);
    }

    private void ensureNotLastActiveAdmin(UserAccount target) {
        if (target.getRole() == Role.ADMINISTRADOR && target.isActive()
                && users.countByRoleAndStatus(Role.ADMINISTRADOR, UserStatus.ACTIVO) <= 1) {
            throw ApiException.conflict("No se puede dejar el sistema sin administradores activos.");
        }
    }
}
