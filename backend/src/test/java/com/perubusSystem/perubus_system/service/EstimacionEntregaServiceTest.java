package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Manifiesto;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EstimacionEntregaServiceTest {

    private final StandardEnvironment environment = new StandardEnvironment();
    private final EstimacionEntregaService service = new EstimacionEntregaService(environment);

    @Test
    void aplicaPlazoConfigurableIndependienteDelSentidoDeLaRuta() {
        environment.getPropertySources().addFirst(new MapPropertySource("eta-test", Map.of(
                "app.tracking.eta-days.default", 2,
                "app.tracking.eta-days.ica-lima", 4)));
        Encomienda encomienda = encomienda("Ica-Lima", LocalDate.of(2026, 11, 10), "En tránsito");

        assertThat(service.calcularFechaEstimada(encomienda))
                .contains(LocalDate.of(2026, 11, 14));
    }

    @Test
    void noCalculaEtaHastaQueElViajeEsteProgramado() {
        Encomienda encomienda = new Encomienda();
        encomienda.setEstadoLogistico("En origen");
        encomienda.setRuta("Lima-Ica");

        assertThat(service.calcularFechaEstimada(encomienda)).isEmpty();
    }

    @Test
    void noMuestraEtaMientrasElEnvioEstePendienteDePago() {
        Encomienda encomienda = encomienda("Lima-Ica", LocalDate.of(2026, 11, 10), "Pendiente de Pago");

        assertThat(service.calcularFechaEstimada(encomienda)).isEmpty();
    }

    private Encomienda encomienda(String ruta, LocalDate salida, String estado) {
        Manifiesto manifiesto = new Manifiesto();
        manifiesto.setFechaViaje(salida);
        Encomienda encomienda = new Encomienda();
        encomienda.setRuta(ruta);
        encomienda.setEstadoLogistico(estado);
        encomienda.setManifiesto(manifiesto);
        return encomienda;
    }
}
