package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Encomienda;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public record PortalEncomiendaRespuesta(
        String codigoTracking,
        String estadoActual,
        String descripcion,
        String ruta,
        String rolCliente,
        String contraparte,
        String fechaViaje,
        String numeroBus,
        LocalDate fechaEstimadaLlegada,
        int progreso,
        boolean estimacionVencida,
        boolean entregado,
        List<EventoTrackingRespuesta> historial) {

    public static PortalEncomiendaRespuesta desde(
            Encomienda encomienda,
            Long clienteId,
            LocalDate fechaEstimadaLlegada,
            List<EventoTrackingRespuesta> historial) {
        boolean esRemitente = encomienda.getRemitente().getIdCliente().equals(clienteId);
        String nombreContraparte = esRemitente
                ? nombre(encomienda.getDestinatario())
                : nombre(encomienda.getRemitente());
        String fechaViaje = encomienda.getManifiesto() == null
                || encomienda.getManifiesto().getFechaViaje() == null
                        ? null
                        : encomienda.getManifiesto().getFechaViaje().toString();
        String numeroBus = encomienda.getManifiesto() == null
                ? null
                : encomienda.getManifiesto().getNumBus();
        String estadoNormalizado = encomienda.getEstadoLogistico() == null
                ? ""
                : encomienda.getEstadoLogistico().trim().toLowerCase(Locale.ROOT);
        boolean entregado = estadoNormalizado.contains("entreg") || estadoNormalizado.contains("recogido");
        boolean llegoADestino = estadoNormalizado.contains("lleg")
                || estadoNormalizado.contains("agencia de destino");
        boolean estimacionVencida = fechaEstimadaLlegada != null
                && fechaEstimadaLlegada.isBefore(LocalDate.now())
                && !llegoADestino
                && !entregado;

        return new PortalEncomiendaRespuesta(
                encomienda.getCodigoTracking(),
                encomienda.getEstadoLogistico(),
                encomienda.getDescripcion(),
                encomienda.getRuta(),
                esRemitente ? "Remitente" : "Destinatario",
                nombreContraparte,
                fechaViaje,
                numeroBus,
                fechaEstimadaLlegada,
                calcularProgreso(estadoNormalizado),
                estimacionVencida,
                entregado,
                historial);
    }

    private static int calcularProgreso(String estado) {
        if (estado.contains("pendiente de pago")) {
            return 0;
        }
        if (estado.contains("entreg") || estado.contains("recogido") || estado.contains("complet")) {
            return 100;
        }
        if (estado.contains("agencia de destino") || estado.equals("llegó a destino")) {
            return 75;
        }
        if (estado.contains("tránsito") || estado.contains("transito")) {
            return 50;
        }
        if (estado.contains("origen")) {
            return 15;
        }
        return 5;
    }

    private static String nombre(com.perubusSystem.perubus_system.model.Cliente cliente) {
        if (cliente == null) {
            return "";
        }
        return ((cliente.getNombres() == null ? "" : cliente.getNombres()) + " "
                + (cliente.getApellidos() == null ? "" : cliente.getApellidos())).trim();
    }
}
