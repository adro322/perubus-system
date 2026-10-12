package com.perubusSystem.perubus_system.dto;

import java.util.List;

public record ManifiestoRespuesta(
        Long idManifiesto,
        String numBus,
        String ruta,
        String fechaViaje,
        Double capacidadKg,
        Double pesoTotalCarga,
        List<Item> encomiendas) {

    public record Item(
            String codigoTracking,
            String descripcion,
            Double peso,
            String remitente,
            String destinatario,
            String estadoLogistico) {
    }
}
