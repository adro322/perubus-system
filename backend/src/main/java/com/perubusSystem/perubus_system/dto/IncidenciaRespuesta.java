package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Incidencia;

import java.time.LocalDateTime;

public record IncidenciaRespuesta(
        Long idIncidencia,
        String codigoTracking,
        String estadoEncomienda,
        String ruta,
        String tipoProblema,
        String descripcion,
        String estado,
        String resolucion,
        LocalDateTime fechaReporte,
        LocalDateTime fechaResolucion) {

    public static IncidenciaRespuesta desde(Incidencia incidencia) {
        Encomienda encomienda = incidencia.getEncomienda();
        String estadoIncidencia = (incidencia.getEstado() == null || incidencia.getEstado().isBlank())
                ? "ABIERTA"
                : incidencia.getEstado().trim().toUpperCase();
        return new IncidenciaRespuesta(
                incidencia.getIdIncidencia(),
                encomienda == null ? null : encomienda.getCodigoTracking(),
                encomienda == null ? null : encomienda.getEstadoLogistico(),
                encomienda == null ? null : encomienda.getRuta(),
                incidencia.getTipoProblema(),
                incidencia.getDescripcion(),
                estadoIncidencia,
                incidencia.getResolucion(),
                incidencia.getFechaReporte(),
                incidencia.getFechaResolucion());
    }
}

