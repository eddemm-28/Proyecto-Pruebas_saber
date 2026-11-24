package co.edu.unicauca.saberpro.client;

import co.edu.unicauca.saberpro.client.controller.AuthController;
import co.edu.unicauca.saberpro.client.controller.QuestionController;
import co.edu.unicauca.saberpro.client.controller.SimulationController;
import co.edu.unicauca.saberpro.client.controller.TrackingController;
import co.edu.unicauca.saberpro.client.controller.UserAdminController;
import co.edu.unicauca.saberpro.client.infra.ApiClient;
import co.edu.unicauca.saberpro.client.model.Session;

/** Composición del cliente (como el AppContext del Corte 1, ahora sobre REST). */
public class AppContext {

    private final Session session = new Session();
    private final ApiClient api;
    private final AuthController auth;
    private final UserAdminController users;
    private final QuestionController questions;
    private final SimulationController simulations;
    private final TrackingController tracking;

    public AppContext(String baseUrl) {
        this.api = new ApiClient(baseUrl, session::token);
        this.auth = new AuthController(api, session);
        this.users = new UserAdminController(api);
        this.questions = new QuestionController(api);
        this.simulations = new SimulationController(api);
        this.tracking = new TrackingController(api);
    }

    /** URL del gateway: -Dsaberpro.api=... o variable SABERPRO_API_URL; por defecto localhost:8080. */
    public static String resolveBaseUrl() {
        String prop = System.getProperty("saberpro.api");
        if (prop != null && !prop.isBlank()) return prop;
        String env = System.getenv("SABERPRO_API_URL");
        if (env != null && !env.isBlank()) return env;
        return ApiClient.DEFAULT_URL;
    }

    public Session session() { return session; }
    public ApiClient api() { return api; }
    public AuthController auth() { return auth; }
    public UserAdminController users() { return users; }
    public QuestionController questions() { return questions; }
    public SimulationController simulations() { return simulations; }
    public TrackingController tracking() { return tracking; }
}
