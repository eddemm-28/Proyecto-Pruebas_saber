package co.edu.unicauca.saberpro.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Acción administrativa sobre un usuario (RF-03, RNF-09). */
@Entity
@Table(name = "user_admin_audit")
public class UserAdminAudit {

    public enum Action { ROLE_CHANGED, ACTIVATED, INACTIVATED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Action action;

    @Column(name = "old_value", length = 50)
    private String oldValue;

    @Column(name = "new_value", length = 50)
    private String newValue;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected UserAdminAudit() {
    }

    public UserAdminAudit(UUID targetUserId, UUID actorUserId, Action action, String oldValue, String newValue) {
        this.targetUserId = targetUserId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.occurredAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getTargetUserId() { return targetUserId; }
    public UUID getActorUserId() { return actorUserId; }
    public Action getAction() { return action; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getOccurredAt() { return occurredAt; }
}
