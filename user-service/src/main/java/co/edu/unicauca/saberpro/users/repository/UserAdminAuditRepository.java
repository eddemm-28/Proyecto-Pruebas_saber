package co.edu.unicauca.saberpro.users.repository;

import co.edu.unicauca.saberpro.users.domain.UserAdminAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAdminAuditRepository extends JpaRepository<UserAdminAudit, Long> {

    List<UserAdminAudit> findByTargetUserIdOrderByOccurredAtDesc(UUID targetUserId);
}
