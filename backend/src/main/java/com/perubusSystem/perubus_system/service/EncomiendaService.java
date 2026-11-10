package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Comprobante;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.EventoTracking;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.ComprobanteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EncomiendaService {

    @Autowired
    private EncomiendaRepository encomiendaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ComprobanteRepository comprobanteRepository;
    @Autowired
    private EventoTrackingRepository eventoTrackingRepository;

    // Lógica CUS-02: Cotizar Envío Dinámico
    public double calcularCotizacion(String origen, String destino, Double peso) {
        double precioPorKilo = 0.0;
        double tarifaBaseMinima = 15.0;

        // Definimos las 5 rutas principales y sus costos por kilo
        String ruta = origen.toUpperCase() + "-" + destino.toUpperCase();

        switch (ruta) {
            case "LIMA-ICA":
            case "ICA-LIMA":
                precioPorKilo = 5.5;
                break;
            case "LIMA-NAZCA":
            case "NAZCA-LIMA":
                precioPorKilo = 7.0;
                break;
            case "ICA-NAZCA":
            case "NAZCA-ICA":
                precioPorKilo = 3.5;
                break;
            case "LIMA-PISCO":
            case "PISCO-LIMA":
                precioPorKilo = 4.5;
                break;
            case "ICA-PISCO":
            case "PISCO-ICA":
                precioPorKilo = 3.0;
                break;
            default:
                throw new IllegalArgumentException("Ruta no disponible para cotización.");
        }

        double costoEstimado = peso * precioPorKilo;
        return Math.max(tarifaBaseMinima, costoEstimado);
    }

    // Lógica CUS-01: Registrar Encomienda
    @Transactional
    public Encomienda registrarEncomienda(Encomienda encomienda) {
        if (encomienda.getRuta() == null || encomienda.getRuta().isBlank()) {
            throw new IllegalArgumentException("La ruta es obligatoria.");
        }
        if (encomienda.getDireccionDestinatario() == null || encomienda.getDireccionDestinatario().isBlank()) {
            throw new IllegalArgumentException("La dirección del destinatario es obligatoria.");
        }
        encomienda.setRuta(encomienda.getRuta().trim());
        encomienda.setDireccionDestinatario(encomienda.getDireccionDestinatario().trim());

        // Validar Usuario
        if (encomienda.getUsuario() != null && encomienda.getUsuario().getIdUsuario() != null) {
            Usuario usuarioBD = usuarioRepository.findById(encomienda.getUsuario().getIdUsuario())
                    .orElseThrow(() -> new RuntimeException("Error: El usuario autenticado no existe en la BD."));
            encomienda.setUsuario(usuarioBD);
        } else {
            throw new RuntimeException("Error: Falta el ID del usuario que registra.");
        }

        // Validar/Guardar Remitente
        if (encomienda.getRemitente() != null) {
            encomienda.getRemitente().setPassword(null);
            Cliente remitente = clienteRepository.findByNumDocumento(encomienda.getRemitente().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getRemitente()));
            encomienda.setRemitente(remitente);
        }

        // Validar/Guardar Destinatario
        if (encomienda.getDestinatario() != null) {
            encomienda.getDestinatario().setPassword(null);
            Cliente destinatario = clienteRepository.findByNumDocumento(encomienda.getDestinatario().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getDestinatario()));
            encomienda.setDestinatario(destinatario);
        }

        // Generar Tracking y Estado INICIAL
        String tracking = "PERU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        encomienda.setCodigoTracking(tracking);

        // ¡CAMBIO CLAVE PARA LA V3! Nace como "Pendiente de Pago"
        encomienda.setEstadoLogistico("Pendiente de Pago");

        Encomienda guardada = encomiendaRepository.save(encomienda);
        registrarEvento(guardada, "Encomienda registrada y pendiente de pago", null);
        return guardada;
    }

    @Transactional
    public Comprobante simularPago(String codigoTracking) {
        Encomienda encomienda = encomiendaRepository.findByCodigoTracking(codigoTracking)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la encomienda."));

        if (!"Pendiente de Pago".equalsIgnoreCase(encomienda.getEstadoLogistico())) {
            throw new IllegalStateException("La encomienda no está pendiente de pago.");
        }

        BigDecimal subtotal = BigDecimal.valueOf(encomienda.getTarifaBase())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv);
        LocalDateTime fechaPago = LocalDateTime.now();

        Comprobante comprobante = new Comprobante();
        comprobante.setEstadoPago("SIMULADO");
        comprobante.setMontoSubtotal(subtotal);
        comprobante.setMontoIgv(igv);
        comprobante.setMontoTotal(total);
        comprobante.setFechaPago(fechaPago);
        comprobante.setEncomienda(encomienda);

        comprobanteRepository.save(comprobante);
        encomienda.setEstadoLogistico("En origen");
        encomiendaRepository.save(encomienda);
        registrarEvento(encomienda, "Pago confirmado. El envío se encuentra en origen.", null);

        return comprobante;
    }

    @Transactional
    public Encomienda confirmarLlegada(String codigoTracking, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el usuario."));
        if (!Boolean.TRUE.equals(usuario.getEstado())
                || !("ADMIN".equalsIgnoreCase(usuario.getRol()) || "AGENTE".equalsIgnoreCase(usuario.getRol()))) {
            throw new IllegalStateException("No tienes permiso para confirmar la llegada.");
        }

        Encomienda encomienda = encomiendaRepository.findByCodigoTracking(codigoTracking)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la encomienda."));
        if (!"En tránsito".equalsIgnoreCase(encomienda.getEstadoLogistico())) {
            throw new IllegalStateException("Solo se puede confirmar la llegada de encomiendas en tránsito.");
        }

        encomienda.setEstadoLogistico("Llegó a agencia de destino");
        encomiendaRepository.save(encomienda);
        registrarEvento(encomienda, "Encomienda recibida en la agencia de destino.", null);
        return encomienda;
    }

    @Transactional
    public Encomienda confirmarEntrega(String codigoTracking, Long usuarioId, String recibidoPor) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el usuario."));
        if (!Boolean.TRUE.equals(usuario.getEstado())
                || !("ADMIN".equalsIgnoreCase(usuario.getRol()) || "AGENTE".equalsIgnoreCase(usuario.getRol()))) {
            throw new IllegalStateException("No tienes permiso para confirmar la entrega.");
        }
        if (recibidoPor == null || recibidoPor.isBlank() || recibidoPor.trim().length() > 120) {
            throw new IllegalArgumentException("Indica el nombre de la persona que recibe la encomienda (máximo 120 caracteres).");
        }

        Encomienda encomienda = encomiendaRepository.findByCodigoTracking(codigoTracking)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la encomienda."));
        if (!"Llegó a agencia de destino".equalsIgnoreCase(encomienda.getEstadoLogistico())) {
            throw new IllegalStateException("La encomienda debe estar en la agencia de destino antes de entregarse.");
        }

        encomienda.setEstadoLogistico("Entregado");
        encomiendaRepository.save(encomienda);
        registrarEvento(encomienda,
                "Entrega confirmada por personal de PeruBus. Recibió: " + recibidoPor.trim() + ".",
                null);
        return encomienda;
    }

    private void registrarEvento(Encomienda encomienda, String detalle, String ubicacion) {
        EventoTracking evento = new EventoTracking();
        evento.setEncomienda(encomienda);
        evento.setEstado(encomienda.getEstadoLogistico());
        evento.setDetalle(detalle);
        evento.setUbicacion(ubicacion);
        eventoTrackingRepository.save(evento);
    }
}