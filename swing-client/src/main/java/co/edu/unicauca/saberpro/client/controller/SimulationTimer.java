package co.edu.unicauca.saberpro.client.controller;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * B14: cronómetro sincronizado con el servidor. El cliente no confía en su reloj: calcula el
 * desfase {@code serverTime - horaLocal} cada vez que recibe datos del servidor y muestra
 * {@code expiresAt - (ahora + desfase)}. La validez real la decide siempre el servidor (RF-23).
 */
public class SimulationTimer {

    private final Clock local;
    private Instant expiresAt;
    private Duration offset = Duration.ZERO;

    public SimulationTimer(Clock local) {
        this.local = local;
    }

    public void sync(Instant serverTime, Instant expiresAt) {
        this.expiresAt = expiresAt;
        if (serverTime != null) {
            this.offset = Duration.between(local.instant(), serverTime);
        }
    }

    public long remainingSeconds() {
        if (expiresAt == null) {
            return 0;
        }
        Instant serverNow = local.instant().plus(offset);
        return Math.max(0, Duration.between(serverNow, expiresAt).getSeconds());
    }

    public boolean isOver() {
        return expiresAt != null && remainingSeconds() == 0;
    }

    public Duration offset() {
        return offset;
    }

    public static String format(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }
}
