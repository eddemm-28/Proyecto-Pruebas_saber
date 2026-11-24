package co.edu.unicauca.saberpro.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Propagación de identidad: elimina cualquier cabecera {@code X-User-*} que venga del cliente
 * (evita suplantación) y la reinyecta desde el JWT ya validado. Además asegura un
 * {@code X-Correlation-Id} para trazar la petición entre servicios y eventos.
 */
@Component
public class UserHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_LOGIN = "X-User-Login";
    public static final String USER_ROLE = "X-User-Role";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        String cid = (correlationId == null || correlationId.isBlank()) ? UUID.randomUUID().toString() : correlationId;

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken())
                .map(jwt -> withHeaders(exchange, cid, jwt))
                .switchIfEmpty(Mono.fromSupplier(() -> withHeaders(exchange, cid, null)))
                .flatMap(chain::filter);
    }

    private ServerWebExchange withHeaders(ServerWebExchange exchange, String cid, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_ID);
                    h.remove(USER_LOGIN);
                    h.remove(USER_ROLE);
                    h.set(CORRELATION_ID, cid);
                    if (jwt != null) {
                        h.set(USER_ID, jwt.getSubject());
                        h.set(USER_LOGIN, String.valueOf(jwt.getClaimAsString("login")));
                        h.set(USER_ROLE, String.valueOf(jwt.getClaimAsString("role")));
                    }
                })
                .build();
        exchange.getResponse().getHeaders().set(CORRELATION_ID, cid);
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
