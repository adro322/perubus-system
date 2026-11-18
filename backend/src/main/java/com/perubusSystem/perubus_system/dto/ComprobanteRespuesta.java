package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Comprobante;
import com.perubusSystem.perubus_system.model.Encomienda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public record ComprobanteRespuesta(
        Long idComprobante,
        String numeroSerie,
        String tipoComprobante,
        String estadoPago,
        BigDecimal montoSubtotal,
        BigDecimal montoIgv,
        BigDecimal montoTotal,
        LocalDateTime fechaEmision,
        LocalDateTime fechaPago,
        String codigoTracking,
        String descripcionEncomienda,
        Double pesoTotal,
        String ruta,
        String clienteNombre,
        String clienteDocumento,
        String destinatarioNombre) {

    public static ComprobanteRespuesta desde(Comprobante comprobante) {
        Encomienda encomienda = comprobante.getEncomienda();
        Cliente remitente = encomienda == null ? null : encomienda.getRemitente();
        Cliente destinatario = encomienda == null ? null : encomienda.getDestinatario();
        String docRemitente = remitente == null || remitente.getNumDocumento() == null
                ? ""
                : remitente.getNumDocumento().trim();

        String tipo = comprobante.getTipoComprobante();
        if (tipo == null || tipo.isBlank()) {
            tipo = docRemitente.length() == 11 ? "FACTURA" : "BOLETA";
        } else {
            tipo = tipo.trim().toUpperCase(Locale.ROOT);
        }

        String serie = comprobante.getNumeroSerie();
        if (serie == null || serie.isBlank()) {
            String prefijo = "FACTURA".equals(tipo) ? "F001-" : "B001-";
            long correlativo = comprobante.getIdComprobante() == null ? 1L : comprobante.getIdComprobante();
            serie = prefijo + String.format(Locale.ROOT, "%06d", correlativo);
        }

        LocalDateTime fechaEmision = comprobante.getFechaEmision() != null
                ? comprobante.getFechaEmision()
                : comprobante.getFechaPago();

        return new ComprobanteRespuesta(
                comprobante.getIdComprobante(),
                serie,
                tipo,
                comprobante.getEstadoPago(),
                comprobante.getMontoSubtotal(),
                comprobante.getMontoIgv(),
                comprobante.getMontoTotal(),
                fechaEmision,
                comprobante.getFechaPago(),
                encomienda == null ? null : encomienda.getCodigoTracking(),
                encomienda == null ? null : encomienda.getDescripcion(),
                encomienda == null ? null : encomienda.getPeso(),
                encomienda == null ? null : encomienda.getRuta(),
                nombreCompleto(remitente),
                docRemitente,
                nombreCompleto(destinatario));
    }

    private static String nombreCompleto(Cliente c) {
        if (c == null) {
            return "";
        }
        return ((c.getNombres() == null ? "" : c.getNombres()) + " "
                + (c.getApellidos() == null ? "" : c.getApellidos())).trim();
    }
}

