package co.edu.unicauca.saberpro.client.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A20: cliente HTTP del gateway ({@code java.net.http.HttpClient} + Jackson). Es el único punto
 * del cliente que conoce URLs; añade el JWT de la sesión y traduce los errores RFC 7807.
 */
public class ApiClient {

    public static final String DEFAULT_URL = "http://localhost:8080";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Supplier<String> tokenSupplier;
    private Runnable onUnauthorized = () -> { };

    public ApiClient(String baseUrl, Supplier<String> tokenSupplier) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), baseUrl, tokenSupplier);
    }

    public ApiClient(HttpClient http, String baseUrl, Supplier<String> tokenSupplier) {
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.tokenSupplier = tokenSupplier;
        this.mapper = mapper();
    }

    public static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** Callback cuando el token expira (401): la UI vuelve al login. */
    public void setOnUnauthorized(Runnable onUnauthorized) {
        this.onUnauthorized = onUnauthorized == null ? () -> { } : onUnauthorized;
    }

    public String baseUrl() {
        return baseUrl;
    }

    // --------------------------------------------------------------- verbos

    public <T> T get(String path, Map<String, ?> query, Class<T> type) {
        return send("GET", path + queryString(query), null, mapper.constructType(type));
    }

    public <T> T get(String path, Map<String, ?> query, TypeReference<T> type) {
        return send("GET", path + queryString(query), null, mapper.getTypeFactory().constructType(type));
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return send("POST", path, body, type == null ? null : mapper.constructType(type));
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return send("PUT", path, body, type == null ? null : mapper.constructType(type));
    }

    public <T> T patch(String path, Object body, Class<T> type) {
        return send("PATCH", path, body, type == null ? null : mapper.constructType(type));
    }

    /** Variante que devuelve status + cuerpo sin lanzar en 409 (útil para "respuesta fuera de tiempo"). */
    public RawResponse putRaw(String path, Object body) {
        HttpResponse<String> resp = execute("PUT", path, body);
        return new RawResponse(resp.statusCode(), resp.body());
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException(0, "Respuesta inválida del servidor", List.of(), json);
        }
    }

    public record RawResponse(int status, String body) {
    }

    // --------------------------------------------------------------- núcleo

    private <T> T send(String method, String path, Object body, JavaType type) {
        HttpResponse<String> resp = execute(method, path, body);
        int status = resp.statusCode();
        if (status >= 200 && status < 300) {
            if (type == null || status == 204 || resp.body() == null || resp.body().isBlank()) {
                return null;
            }
            try {
                return mapper.readValue(resp.body(), type);
            } catch (IOException e) {
                throw new ApiException(status, "No se pudo interpretar la respuesta del servidor", List.of(), resp.body());
            }
        }
        ApiException error = toApiException(status, resp.body());
        if (status == 401 && tokenSupplier.get() != null) {
            onUnauthorized.run();
        }
        throw error;
    }

    private HttpResponse<String> execute(String method, String path, Object body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");
            String token = tokenSupplier.get();
            if (token != null) {
                b.header("Authorization", "Bearer " + token);
            }
            if (body != null) {
                b.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8));
            } else if ("GET".equals(method)) {
                b.GET();
            } else {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            }
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ApiException(0, "No hay conexión con el servidor (" + baseUrl + "). ¿Está levantado docker compose?",
                    List.of(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Operación interrumpida", List.of(), null);
        }
    }

    ApiException toApiException(int status, String body) {
        String message = switch (status) {
            case 401 -> "Sesión no válida o expirada. Inicie sesión de nuevo.";
            case 403 -> "No tiene permisos para esta operación.";
            case 404 -> "Recurso no encontrado.";
            case 502, 503, 504 -> "El servicio no está disponible en este momento.";
            default -> "Error " + status;
        };
        List<String> errors = new ArrayList<>();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = mapper.readTree(body);
                if (node.hasNonNull("detail")) {
                    message = node.get("detail").asText();
                }
                if (node.has("errors") && node.get("errors").isArray()) {
                    node.get("errors").forEach(e -> errors.add(e.asText()));
                }
            } catch (IOException ignored) {
                // cuerpo no JSON: se deja el mensaje genérico
            }
        }
        return new ApiException(status, message, errors, body);
    }

    static String queryString(Map<String, ?> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> e : new LinkedHashMap<>(query).entrySet()) {
            if (e.getValue() == null || e.getValue().toString().isBlank()) {
                continue;
            }
            sb.append(sb.length() == 0 ? '?' : '&')
                    .append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
