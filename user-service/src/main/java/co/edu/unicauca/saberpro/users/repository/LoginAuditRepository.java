package co.edu.unicauca.saberpro.users.repository;

import co.edu.unicauca.saberpro.users.domain.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long>, JpaSpecificationExecutor<LoginAudit> {
}
