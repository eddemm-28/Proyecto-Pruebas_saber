package co.edu.unicauca.saberpro.users.web;

import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserStatus;
import co.edu.unicauca.saberpro.users.service.LoginAuditService;
import co.edu.unicauca.saberpro.users.service.UserAdminService;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.AdminAuditResponse;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.ChangeRoleRequest;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.ChangeStatusRequest;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.LoginAuditResponse;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.PageResponse;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A09 (RF-03) y A11 (RNF-09). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAdminService adminService;
    private final LoginAuditService auditService;

    public UserController(UserAdminService adminService, LoginAuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return UserResponse.from(adminService.get(CurrentUser.from(auth).id()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PageResponse<UserResponse> list(@RequestParam(required = false) Role role,
                                           @RequestParam(required = false) UserStatus status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("login"));
        return PageResponse.of(adminService.search(role, status, q, pr), UserResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(adminService.get(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UserResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest req, Authentication auth) {
        return UserResponse.from(adminService.changeStatus(id, req.status(), CurrentUser.from(auth).id()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest req, Authentication auth) {
        return UserResponse.from(adminService.changeRole(id, req.role(), CurrentUser.from(auth).id()));
    }

    @GetMapping("/{id}/admin-audit")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<AdminAuditResponse> adminHistory(@PathVariable UUID id) {
        return adminService.history(id).stream().map(AdminAuditResponse::from).toList();
    }

    @GetMapping("/audit/logins")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PageResponse<LoginAuditResponse> loginAudit(
            @RequestParam(required = false) String login,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        return PageResponse.of(auditService.search(login, success, from, to, pr), LoginAuditResponse::from);
    }
}
