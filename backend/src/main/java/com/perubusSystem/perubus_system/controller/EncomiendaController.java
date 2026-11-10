package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.service.EncomiendaService;
import com.perubusSystem.perubus_system.dto.ClienteResumen;
import com.perubusSystem.perubus_system.dto.ConfirmacionEntregaSolicitud;
import com.perubusSystem.perubus_system.dto.EncomiendaConsulta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.Locale;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/encomiendas")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class EncomiendaController {

    @Autowired
    private EncomiendaRepository encomiendaRepository;

    @Autowired
    private EncomiendaService encomiendaService;

    // ==========================================
    // CUS-02: COTIZAR ENVÍO
    // ==========================================
    @PostMapping("/cotizar")
    public ResponseEntity<?> cotizarEnvio(@RequestBody Map<String, Object> datosCotizacion) {
        try {
            Double peso = Double.valueOf(datosCotizacion.get("peso").toString());
            // Ahora el frontend también debe enviarnos origen y destino
            String origen = datosCotizacion.get("origen").toString();
            String destino = datosCotizacion.get("destino").toString();

            double costoEstimado = encomiendaService.calcularCotizacion(origen, destino, peso);
            return ResponseEntity.ok(Map.of("costoEstimado", costoEstimado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error en la cotización: " + e.getMessage());
        }
    }

    // ==========================================
    // CUS-01: REGISTRAR ENCOMIENDA
    // ==========================================
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarEncomienda(@RequestBody Encomienda encomienda) {
        try {
            // Toda la lógica pesada ahora vive en el Service
            Encomienda encomiendaGuardada = encomiendaService.registrarEncomienda(encomienda);
            return ResponseEntity.ok(encomiendaGuardada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // CONSULTAS (Get)
    // ==========================================
    @GetMapping
    public ResponseEntity<?> listarEncomiendas() {
        return ResponseEntity.ok(encomiendaRepository.findAll().stream()
                .map(this::convertirAConsulta)
                .toList());
    }

    @GetMapping("/buscar/{codigoTracking}")
    public ResponseEntity<?> consultarEncomienda(@PathVariable String codigoTracking) {
        String trackingNormalizado = codigoTracking == null
                ? ""
                : codigoTracking.trim().toUpperCase(Locale.ROOT);

        if (!trackingNormalizado.matches("PERU-[A-Z0-9]{8}")) {
            return ResponseEntity.badRequest().body("El código de tracking no tiene un formato válido.");
        }

        return encomiendaRepository.findByCodigoTracking(trackingNormalizado)
                .map(encomienda -> ResponseEntity.ok(convertirAConsulta(encomienda)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{codigoTracking}/simular-pago")
    public ResponseEntity<?> simularPago(@PathVariable String codigoTracking) {
        try {
            var comprobante = encomiendaService.simularPago(codigoTracking.trim().toUpperCase(Locale.ROOT));
            return ResponseEntity.ok(Map.of(
                    "codigoTracking", comprobante.getEncomienda().getCodigoTracking(),
                    "estadoLogistico", comprobante.getEncomienda().getEstadoLogistico(),
                    "estadoPago", comprobante.getEstadoPago(),
                    "subtotal", comprobante.getMontoSubtotal(),
                    "igv", comprobante.getMontoIgv(),
                    "total", comprobante.getMontoTotal(),
                    "fechaPago", comprobante.getFechaPago()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/{codigoTracking}/confirmar-llegada")
    public ResponseEntity<?> confirmarLlegada(
            @PathVariable String codigoTracking,
            HttpSession session) {
        Object usuarioId = session.getAttribute("idUsuario");
        if (!(usuarioId instanceof Long id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Inicia sesión como personal para confirmar la llegada."));
        }
        try {
            Encomienda encomienda = encomiendaService.confirmarLlegada(
                    codigoTracking.trim().toUpperCase(Locale.ROOT), id);
            return ResponseEntity.ok(Map.of(
                    "codigoTracking", encomienda.getCodigoTracking(),
                    "estadoLogistico", encomienda.getEstadoLogistico()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/{codigoTracking}/confirmar-entrega")
    public ResponseEntity<?> confirmarEntrega(
            @PathVariable String codigoTracking,
            @RequestBody ConfirmacionEntregaSolicitud solicitud,
            HttpSession session) {
        Object usuarioId = session.getAttribute("idUsuario");
        if (!(usuarioId instanceof Long id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Inicia sesión como personal para confirmar la entrega."));
        }
        try {
            Encomienda encomienda = encomiendaService.confirmarEntrega(
                    codigoTracking.trim().toUpperCase(Locale.ROOT), id,
                    solicitud == null ? null : solicitud.recibidoPor());
            return ResponseEntity.ok(Map.of(
                    "codigoTracking", encomienda.getCodigoTracking(),
                    "estadoLogistico", encomienda.getEstadoLogistico()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // Métodos DTO privados (Estos se quedan aquí porque son para transformar la
    // salida del endpoint)
    private EncomiendaConsulta convertirAConsulta(Encomienda encomienda) {
        String fechaViaje = encomienda.getManifiesto() == null || encomienda.getManifiesto().getFechaViaje() == null
                ? null
                : encomienda.getManifiesto().getFechaViaje().toString();
        String numeroBus = encomienda.getManifiesto() == null ? null : encomienda.getManifiesto().getNumBus();

        return new EncomiendaConsulta(
                encomienda.getCodigoTracking(),
                encomienda.getPeso(),
                encomienda.getEstadoLogistico(),
                encomienda.getDescripcion(),
                encomienda.getTarifaBase(),
                convertirCliente(encomienda.getRemitente()),
                convertirCliente(encomienda.getDestinatario()),
                encomienda.getRuta(),
                encomienda.getDireccionDestinatario(),
                fechaViaje,
                numeroBus);
    }

    private ClienteResumen convertirCliente(Cliente cliente) {
        if (cliente == null)
            return null;
        return new ClienteResumen(
                cliente.getNombres(), cliente.getApellidos(),
                cliente.getNumDocumento(), cliente.getTelefono(), cliente.getCorreo());
    }
}