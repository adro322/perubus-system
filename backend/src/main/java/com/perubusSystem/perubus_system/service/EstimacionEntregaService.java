package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Encomienda;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
public class EstimacionEntregaService {

    private final Environment environment;

    public EstimacionEntregaService(Environment environment) {
        this.environment = environment;
    }

    public Optional<LocalDate> calcularFechaEstimada(Encomienda encomienda) {
        if (encomienda.getEstadoLogistico() != null
                && encomienda.getEstadoLogistico().equalsIgnoreCase("Pendiente de Pago")) {
            return Optional.empty();
        }
        if (encomienda.getManifiesto() == null || encomienda.getManifiesto().getFechaViaje() == null) {
            return Optional.empty();
        }

        String ruta = encomienda.getRuta();
        if (ruta == null || ruta.isBlank()) {
            return Optional.empty();
        }

        String[] tramos = Normalizer.normalize(ruta, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .split("\\s*-\\s*");
        if (tramos.length != 2 || tramos[0].isBlank() || tramos[1].isBlank()) {
            return Optional.empty();
        }
        Arrays.sort(tramos);

        String claveRuta = String.join("-", tramos);
        int diasEstimados = environment.getProperty(
                "app.tracking.eta-days." + claveRuta,
                Integer.class,
                environment.getProperty("app.tracking.eta-days.default", Integer.class, 2));
        if (diasEstimados < 1 || diasEstimados > 7) {
            throw new IllegalStateException("El plazo estimado de la ruta " + claveRuta
                    + " debe estar entre 1 y 7 días.");
        }

        return Optional.of(encomienda.getManifiesto().getFechaViaje().plusDays(diasEstimados));
    }
}
