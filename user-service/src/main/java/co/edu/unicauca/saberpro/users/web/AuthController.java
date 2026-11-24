package co.edu.unicauca.saberpro.users.web;

import co.edu.unicauca.saberpro.users.service.AuthService;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.LoginRequest;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.LoginResponse;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.RegisterRequest;
import co.edu.unicauca.saberpro.users.web.dto.UserDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF-01 y RF-02. Rutas públicas. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        return UserResponse.from(authService.register(req.login(), req.fullName(), req.email(), req.password()));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        AuthService.LoginResult result = authService.login(req.login(), req.password(),
                clientIp(http), http.getHeader(HttpHeaders.USER_AGENT));
        return new LoginResponse(result.token().value(), "Bearer", result.token().expiresInSeconds(),
                result.token().expiresAt(), UserResponse.from(result.user()));
    }

    /** El gateway añade X-Forwarded-For; con forward-headers-strategy=framework getRemoteAddr ya lo refleja. */
    private static String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
