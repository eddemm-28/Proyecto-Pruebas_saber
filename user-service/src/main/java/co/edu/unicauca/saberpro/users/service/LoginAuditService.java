package co.edu.unicauca.saberpro.users.service;

import co.edu.unicauca.saberpro.users.domain.LoginAudit;
import co.edu.unicauca.saberpro.users.repository.LoginAuditRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A11 / RNF-09: auditoría de inicios de sesión. Se graba en una transacción propia
 * ({@code REQUIRES_NEW}) para que los intentos fallidos queden registrados aunque el login falle.
 */
@Service
public class LoginAuditService {

    private final LoginAuditRepository repository;

    public LoginAuditService(LoginAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LoginAudit entry) {
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<LoginAudit> search(String login, Boolean success, Instant from, Instant to, Pageable pageable) {
        Specification<LoginAudit> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (login != null && !login.isBlank()) {
                p.add(cb.like(cb.lower(root.get("loginAttempted")), "%" + login.toLowerCase().trim() + "%"));
            }
            if (success != null) {
                p.add(cb.equal(root.get("success"), success));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, pageable);
    }
}
