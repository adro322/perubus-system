package com.perubusSystem.perubus_system.seguridad;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorIntentosTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000L);
    // 3 fallos en 15 minutos bloquean 10 minutos
    private final LimitadorIntentos limitador = new LimitadorIntentos(3, 15 * 60_000L, 10 * 60_000L, ahora::get);

    @Test
    void bloqueaTrasLosFallosPermitidos() {
        limitador.fallo("a");
        limitador.fallo("a");
        assertThat(limitador.bloqueado("a")).isFalse();

        limitador.fallo("a");

        assertThat(limitador.bloqueado("a")).isTrue();
        assertThat(limitador.minutosRestantes("a")).isEqualTo(10);
    }

    @Test
    void elBloqueoSeLevantaConElTiempo() {
        for (int i = 0; i < 3; i++) limitador.fallo("a");
        assertThat(limitador.bloqueado("a")).isTrue();

        ahora.addAndGet(10 * 60_000L + 1);

        assertThat(limitador.bloqueado("a")).isFalse();
    }

    @Test
    void unaClaveNoAfectaAOtra() {
        for (int i = 0; i < 3; i++) limitador.fallo("a");

        assertThat(limitador.bloqueado("a")).isTrue();
        assertThat(limitador.bloqueado("b")).isFalse();
    }

    @Test
    void unIngresoCorrectoReiniciaLaCuenta() {
        limitador.fallo("a");
        limitador.fallo("a");
        limitador.exito("a");
        limitador.fallo("a");
        limitador.fallo("a");

        assertThat(limitador.bloqueado("a")).isFalse();
    }

    @Test
    void losFallosViejosNoSeAcumulan() {
        limitador.fallo("a");
        limitador.fallo("a");
        ahora.addAndGet(16 * 60_000L); // fuera de la ventana de 15 minutos

        limitador.fallo("a");

        assertThat(limitador.bloqueado("a")).isFalse();
    }
}
