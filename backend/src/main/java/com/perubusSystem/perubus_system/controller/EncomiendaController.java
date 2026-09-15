package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import com.perubusSystem.perubus_system.dto.ClienteResumen;
import com.perubusSystem.perubus_system.dto.EncomiendaConsulta;

import java.util.Map;
import java.util.UUID;
import java.util.Locale;

@RestController
@RequestMapping("/api/encomiendas")
@CrossOrigin(origins = "*")
public class EncomiendaController {

    @Autowired
    private EncomiendaRepository encomiendaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==========================================
    // CUS-02: COTIZAR ENVÍO
    // ==========================================
    @PostMapping("/cotizar")
    public ResponseEntity<?> cotizarEnvio(@RequestBody Map<String, Object> datosCotizacion) {

        Double peso = Double.valueOf(datosCotizacion.get("peso").toString());

        // Reglas de negocio de PeruBus
        double tarifaBaseMinima = 15.0;
        double costoPorKilo = 5.5;

        double costoEstimado = Math.max(tarifaBaseMinima, peso * costoPorKilo);

        return ResponseEntity.ok(Map.of("costoEstimado", costoEstimado));
    }

    // ==========================================
    // CUS-01: REGISTRAR ENCOMIENDA
    // ==========================================
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarEncomienda(@RequestBody Encomienda encomienda) {

        if (encomienda.getUsuario() != null && encomienda.getUsuario().getIdUsuario() != null) {
            Usuario usuarioBD = usuarioRepository.findById(encomienda.getUsuario().getIdUsuario())
                    .orElseThrow(() -> new RuntimeException("Error: El usuario autenticado no existe en la BD."));
            encomienda.setUsuario(usuarioBD);
        } else {
            return ResponseEntity.badRequest().body("Error: Falta el ID del usuario que registra.");
        }

        if (encomienda.getRemitente() != null) {
            Cliente remitente = clienteRepository.findByNumDocumento(encomienda.getRemitente().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getRemitente()));
            encomienda.setRemitente(remitente);
        }

        if (encomienda.getDestinatario() != null) {
            Cliente destinatario = clienteRepository.findByNumDocumento(encomienda.getDestinatario().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getDestinatario()));
            encomienda.setDestinatario(destinatario);
        }

        String tracking = "PERU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        encomienda.setCodigoTracking(tracking);
        encomienda.setEstadoLogistico("En origen");

        Encomienda encomiendaGuardada = encomiendaRepository.save(encomienda);

        return ResponseEntity.ok(encomiendaGuardada);
    }

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

    private EncomiendaConsulta convertirAConsulta(Encomienda encomienda) {
        String ruta = encomienda.getManifiesto() == null ? null : encomienda.getManifiesto().getRuta();
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
                ruta,
                fechaViaje,
                numeroBus);
    }

    private ClienteResumen convertirCliente(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        return new ClienteResumen(
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getNumDocumento(),
                cliente.getTelefono(),
                cliente.getCorreo());
    }
}