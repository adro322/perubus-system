package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.SolicitudEncomienda;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record SolicitudEncomiendaRespuesta(
        Long idSolicitud,
        String codigoTracking,
        String tipo,
        String direccionActual,
        String direccionSolicitada,
        String descripcionBulto,
        Double pesoBulto,
        BigDecimal montoAdicional,
        LocalDateTime fechaPagoAdicional,
        String estado,
        String respuesta,
        String cliente,
        LocalDateTime fechaSolicitud) {

    public static SolicitudEncomiendaRespuesta desde(SolicitudEncomienda solicitud) {
        String nombreCliente = solicitud.getCliente().getNombres() + " "
                + (solicitud.getCliente().getApellidos() == null ? "" : solicitud.getCliente().getApellidos());
        return new SolicitudEncomiendaRespuesta(
                solicitud.getIdSolicitud(),
                solicitud.getEncomienda().getCodigoTracking(),
                solicitud.getTipo(),
                solicitud.getEncomienda().getDireccionDestinatario(),
                solicitud.getDireccionSolicitada(),
                solicitud.getDescripcionBulto(),
                solicitud.getPesoBulto(),
                solicitud.getMontoAdicional(),
                solicitud.getFechaPagoAdicional(),
                solicitud.getEstado(),
                solicitud.getRespuesta(),
                nombreCliente.trim(),
                solicitud.getFechaSolicitud());
    }
}
