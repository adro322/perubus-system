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
    // CUS-02: COTIZAR ENVÍO (Solo calcula, no guarda en BD)
    // ==========================================
    @PostMapping("/cotizar")
    public ResponseEntity<?> cotizarEnvio(@RequestBody Map<String, Object> datosCotizacion) {
        // Obtenemos el peso enviado desde React
        Double peso = Double.valueOf(datosCotizacion.get("peso").toString());

        // Reglas de negocio de PeruBus
        double tarifaBaseMinima = 15.0; // Lo mínimo que se cobra por cualquier envío pequeño
        double costoPorKilo = 5.5; // Costo por kilo

        // El sistema calcula el costo por peso, pero si es menor al mínimo, aplica la
        // base.
        double costoEstimado = Math.max(tarifaBaseMinima, peso * costoPorKilo);

        return ResponseEntity.ok(Map.of("costoEstimado", costoEstimado));
    }

    // ==========================================
    // CUS-01: REGISTRAR ENCOMIENDA (Sí guarda en BD)
    // ==========================================
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarEncomienda(@RequestBody Encomienda encomienda) {

        // 1. Validar Usuario (El ID viene desde React)
        if (encomienda.getUsuario() != null && encomienda.getUsuario().getIdUsuario() != null) {
            Usuario usuarioBD = usuarioRepository.findById(encomienda.getUsuario().getIdUsuario())
                    .orElseThrow(() -> new RuntimeException("Error: El usuario autenticado no existe en la BD."));
            encomienda.setUsuario(usuarioBD);
        } else {
            return ResponseEntity.badRequest().body("Error: Falta el ID del usuario que registra.");
        }

        // 2. Validamos y guardamos el REMITENTE
        if (encomienda.getRemitente() != null) {
            Cliente remitente = clienteRepository.findByNumDocumento(encomienda.getRemitente().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getRemitente()));
            encomienda.setRemitente(remitente);
        }

        // 3. Validamos y guardamos el DESTINATARIO
        if (encomienda.getDestinatario() != null) {
            Cliente destinatario = clienteRepository.findByNumDocumento(encomienda.getDestinatario().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getDestinatario()));
            encomienda.setDestinatario(destinatario);
        }

        // 4. Generar Tracking y Estado Inicial
        String tracking = "PERU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        encomienda.setCodigoTracking(tracking);
        encomienda.setEstadoLogistico("En origen");

        // 5. Guardar todo en PostgreSQL (Supabase)
        Encomienda encomiendaGuardada = encomiendaRepository.save(encomienda);

        return ResponseEntity.ok(encomiendaGuardada);
    }

    // Para ver el historial de encomiendas
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