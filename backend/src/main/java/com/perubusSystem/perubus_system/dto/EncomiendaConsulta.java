package com.perubusSystem.perubus_system.dto;

public record EncomiendaConsulta(
        String codigoTracking,
        Double peso,
        String estadoLogistico,
        String descripcion,
        Double tarifaBase,
        ClienteResumen remitente,
        ClienteResumen destinatario,
        String ruta,
        String fechaViaje,
        String numeroBus) {
}
