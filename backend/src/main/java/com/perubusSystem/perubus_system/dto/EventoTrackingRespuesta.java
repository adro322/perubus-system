package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.EventoTracking;

import java.time.LocalDateTime;

public record EventoTrackingRespuesta(
        String estado,
        String detalle,
        String ubicacion,
        LocalDateTime fecha) {

    public static EventoTrackingRespuesta desde(EventoTracking evento) {
        return new EventoTrackingRespuesta(
                evento.getEstado(),
                evento.getDetalle(),
                evento.getUbicacion(),
                evento.getFecha());
    }
}
