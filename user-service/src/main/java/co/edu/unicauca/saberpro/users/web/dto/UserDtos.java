package co.edu.unicauca.saberpro.users.web.dto;

import co.edu.unicauca.saberpro.users.domain.LoginAudit;
import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserAccount;
import co.edu.unicauca.saberpro.users.domain.UserAdminAudit;
import co.edu.unicauca.saberpro.users.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** DTOs REST del user-service. */
public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterRequest(@NotBlank String login, @NotBlank String fullName,
                                  @NotBlank String email, @NotBlank String password) {
    }

    public record LoginRequest(@NotBlank String login, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt, UserResponse user) {
    }

    public record UserResponse(UUID id, String login, String fullName, String email, Role role, String roleName,
                               UserStatus status, Instant createdAt) {
        public static UserResponse from(UserAccount u) {
            return new UserResponse(u.getId(), u.getLogin(), u.getFullName(), u.getEmail(), u.getRole(),
                    u.getRole().displayName(), u.getStatus(), u.getCreatedAt());
        }
    }

    public record ChangeStatusRequest(@NotNull UserStatus status) {
    }

    public record ChangeRoleRequest(@NotNull Role role) {
    }

    public record LoginAuditResponse(Long id, UUID userId, String login, boolean success, String failureReason,
                                     String ipAddress, String clientAgent, Instant occurredAt) {
        public static LoginAuditResponse from(LoginAudit a) {
            return new LoginAuditResponse(a.getId(), a.getUserId(), a.getLoginAttempted(), a.isSuccess(),
                    a.getFailureReason() == null ? null : a.getFailureReason().name(),
                    a.getIpAddress(), a.getClientAgent(), a.getOccurredAt());
        }
    }

    public record AdminAuditResponse(Long id, UUID targetUserId, UUID actorUserId, String action,
                                     String oldValue, String newValue, Instant occurredAt) {
        public static AdminAuditResponse from(UserAdminAudit a) {
            return new AdminAuditResponse(a.getId(), a.getTargetUserId(), a.getActorUserId(), a.getAction().name(),
                    a.getOldValue(), a.getNewValue(), a.getOccurredAt());
        }
    }

    /** Formato de página estable para el cliente Swing (no depende de la serialización de Spring Data). */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
