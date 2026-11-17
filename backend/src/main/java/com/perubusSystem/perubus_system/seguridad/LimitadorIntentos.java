package com.perubusSystem.perubus_system.seguridad;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

// Bloquea temporalmente una clave (por ejemplo un documento o usuario) tras varios fallos seguidos.
// Vive en memoria: se reinicia al reiniciar el servidor.
public class LimitadorIntentos {

    private static final int LIMITE_DE_CLAVES = 10_000;

    private static class Estado {
        int fallos;
        long inicio;
        long bloqueadoHasta;
    }

    private final int maxFallos;
    private final long ventanaMs;
    private final long bloqueoMs;
    private final LongSupplier reloj;
    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    // 5 fallos en 15 minutos bloquean durante 10 minutos
    public LimitadorIntentos() {
        this(5, 15 * 60_000L, 10 * 60_000L, System::currentTimeMillis);
    }

    public LimitadorIntentos(int maxFallos, long ventanaMs, long bloqueoMs, LongSupplier reloj) {
        this.maxFallos = maxFallos;
        this.ventanaMs = ventanaMs;
        this.bloqueoMs = bloqueoMs;
        this.reloj = reloj;
    }

    public synchronized boolean bloqueado(String clave) {
        Estado estado = estados.get(clave);
        return estado != null && reloj.getAsLong() < estado.bloqueadoHasta;
    }

    public synchronized long minutosRestantes(String clave) {
        Estado estado = estados.get(clave);
        if (estado == null) return 0;
        long restante = estado.bloqueadoHasta - reloj.getAsLong();
        return restante <= 0 ? 0 : (restante + 59_999) / 60_000;
    }

    public synchronized void fallo(String clave) {
        long ahora = reloj.getAsLong();
        if (estados.size() > LIMITE_DE_CLAVES) {
            estados.values().removeIf(e -> ahora >= e.bloqueadoHasta && ahora - e.inicio > ventanaMs);
        }
        Estado estado = estados.computeIfAbsent(clave, k -> new Estado());
        if (estado.fallos == 0 || ahora - estado.inicio > ventanaMs) {
            estado.fallos = 0;
            estado.inicio = ahora;
        }
        estado.fallos++;
        if (estado.fallos >= maxFallos) {
            estado.bloqueadoHasta = ahora + bloqueoMs;
            estado.fallos = 0;
        }
    }

    public synchronized void exito(String clave) {
        estados.remove(clave);
    }
}
