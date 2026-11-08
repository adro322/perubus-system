package com.perubusSystem.perubus_system.dto;

import java.time.LocalDate;

public record ManifiestoSolicitud(
        String numBus,
        String ruta,
        LocalDate fechaViaje,
        Double capacidadKg) {
}
