package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Encomienda;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public record PortalEncomiendaRespuesta(
        String codigoTracking,
        String estadoActual,
        String descripcion,
        Double pesoTotal,
        Double tarifaTotal,
        List<BultoEncomiendaRespuesta> bultos,
        String direccionDestinatario,
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
            List<EventoTrackingRespuesta> historial,
            List<BultoEncomiendaRespuesta> bultos) {
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
        boolean cancelado = estadoNormalizado.contains("cancel");
        boolean llegoADestino = estadoNormalizado.contains("agencia de destino")
                || estadoNormalizado.contains("disponible para recojo")
                || estadoNormalizado.contains("reparto");
        boolean estimacionVencida = fechaEstimadaLlegada != null
                && fechaEstimadaLlegada.isBefore(LocalDate.now())
                && !llegoADestino
                && !entregado
                && !cancelado;

        return new PortalEncomiendaRespuesta(
                encomienda.getCodigoTracking(),
                encomienda.getEstadoLogistico(),
                encomienda.getDescripcion(),
                encomienda.getPeso(),
                encomienda.getTarifaBase(),
                bultos,
                encomienda.getDireccionDestinatario(),
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

    public static PortalEncomiendaRespuesta desde(
            Encomienda encomienda,
            Long clienteId,
            LocalDate fechaEstimadaLlegada,
            List<EventoTrackingRespuesta> historial) {
        Double pesoOriginal = encomienda.getPesoOriginal() == null
                ? encomienda.getPeso()
                : encomienda.getPesoOriginal();
        return desde(encomienda, clienteId, fechaEstimadaLlegada, historial, List.of(
                new BultoEncomiendaRespuesta(encomienda.getDescripcion(), pesoOriginal, true)));
    }

    private static int calcularProgreso(String estado) {
        if (estado.contains("cancel")) {
            return 0;
        }
        if (estado.contains("entreg") || estado.contains("recogido") || estado.contains("complet")) {
            return 100;
        }
        if (estado.contains("disponible para recojo") || estado.contains("reparto")) {
            return 80;
        }
        if (estado.contains("pendiente de pago adicional")) {
            return 20;
        }
        if (estado.contains("agencia de destino") || estado.equals("llegó a destino")) {
            return 60;
        }
        if (estado.contains("tránsito") || estado.contains("transito")) {
            return 40;
        }
        if (estado.contains("origen")) {
            return 20;
        }
        return 0;
    }

    private static String nombre(com.perubusSystem.perubus_system.model.Cliente cliente) {
        if (cliente == null) {
            return "";
        }
        return ((cliente.getNombres() == null ? "" : cliente.getNombres()) + " "
                + (cliente.getApellidos() == null ? "" : cliente.getApellidos())).trim();
    }
}
