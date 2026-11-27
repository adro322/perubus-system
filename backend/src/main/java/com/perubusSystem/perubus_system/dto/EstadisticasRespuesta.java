package com.perubusSystem.perubus_system.dto;

import java.math.BigDecimal;
import java.util.List;

public record EstadisticasRespuesta(
        long totalEncomiendas,
        long encomiendasEntregadas,
        long encomiendasEnCurso,
        long encomiendasPendientesPago,
        long encomiendasCanceladas,
        double pesoTotalTransportadoKg,
        BigDecimal ingresosSubtotal,
        BigDecimal ingresosIgv,
        BigDecimal ingresosTotales,
        long totalComprobantes,
        long totalBoletas,
        long totalFacturas,
        long totalClientes,
        long clientesConPortalActivo,
        long totalManifiestos,
        double ocupacionPromedioBuses,
        long incidenciasAbiertas,
        long incidenciasResueltas,
        long solicitudesPendientes,
        List<ItemEstado> porEstado,
        List<ItemRuta> porRuta,
        List<ComprobanteRespuesta> ultimosComprobantes) {

    public record ItemEstado(
            String estado,
            long cantidad,
            double porcentaje) {
    }

    public record ItemRuta(
            String ruta,
            long cantidadEncomiendas,
            double pesoTotalKg,
            BigDecimal ingresosSubtotal,
            BigDecimal ingresosTotal) {
    }
}

