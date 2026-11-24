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

/** Registro de un intento de inicio de sesión (RNF-09, A11). */
@Entity
@Table(name = "login_audit")
public class LoginAudit {

    public enum FailureReason { BAD_CREDENTIALS, USER_INACTIVE, USER_NOT_FOUND }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "login_attempted", nullable = false, length = 100)
    private String loginAttempted;

    @Column(nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 30)
    private FailureReason failureReason;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "client_agent", length = 255)
    private String clientAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LoginAudit() {
    }

    private LoginAudit(UUID userId, String loginAttempted, boolean success, FailureReason reason,
                       String ipAddress, String clientAgent) {
        this.userId = userId;
        this.loginAttempted = truncate(loginAttempted, 100);
        this.success = success;
        this.failureReason = reason;
        this.ipAddress = truncate(ipAddress, 64);
        this.clientAgent = truncate(clientAgent, 255);
        this.occurredAt = Instant.now();
    }

    public static LoginAudit success(UUID userId, String login, String ip, String agent) {
        return new LoginAudit(userId, login, true, null, ip, agent);
    }

    public static LoginAudit failure(UUID userId, String login, FailureReason reason, String ip, String agent) {
        return new LoginAudit(userId, login, false, reason, ip, agent);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getLoginAttempted() { return loginAttempted; }
    public boolean isSuccess() { return success; }
    public FailureReason getFailureReason() { return failureReason; }
    public String getIpAddress() { return ipAddress; }
    public String getClientAgent() { return clientAgent; }
    public Instant getOccurredAt() { return occurredAt; }
}
