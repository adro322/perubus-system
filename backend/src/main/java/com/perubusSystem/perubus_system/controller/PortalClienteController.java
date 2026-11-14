package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.*;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.BultoEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.service.EstimacionEntregaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/portal")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class PortalClienteController {

    private static final String SESION_CLIENTE_ID = "portalClienteId";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final ClienteRepository clienteRepository;
    private final BultoEncomiendaRepository bultoRepository;
    private final EncomiendaRepository encomiendaRepository;
    private final EventoTrackingRepository eventoTrackingRepository;
    private final EstimacionEntregaService estimacionEntregaService;

    public PortalClienteController(
            ClienteRepository clienteRepository,
            BultoEncomiendaRepository bultoRepository,
            EncomiendaRepository encomiendaRepository,
            EventoTrackingRepository eventoTrackingRepository,
            EstimacionEntregaService estimacionEntregaService) {
        this.clienteRepository = clienteRepository;
        this.bultoRepository = bultoRepository;
        this.encomiendaRepository = encomiendaRepository;
        this.eventoTrackingRepository = eventoTrackingRepository;
        this.estimacionEntregaService = estimacionEntregaService;
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody PortalRegistroSolicitud solicitud) {
        String documento = normalizar(solicitud.numDocumento());
        String correo = normalizar(solicitud.correo());
        correo = correo == null ? "" : correo.toLowerCase(Locale.ROOT);
        String password = solicitud.password() == null ? "" : solicitud.password();

        if (documento == null || !documento.matches("\\d{8}|\\d{11}")) {
            return error(HttpStatus.BAD_REQUEST, "El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC).");
        }
        if (correo.isBlank() || !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return error(HttpStatus.BAD_REQUEST, "Ingrese un correo electrónico válido.");
        }
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return error(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres y no superar 72 bytes UTF-8.");
        }
        String telefono = normalizar(solicitud.telefono());
        if (telefono != null && !telefono.isBlank() && !telefono.matches("\\d{9}")) {
            return error(HttpStatus.BAD_REQUEST, "El teléfono debe tener 9 dígitos.");
        }

        Optional<Cliente> clienteExistente = clienteRepository.findByNumDocumento(documento);
        Cliente cliente;
        if (clienteExistente.isPresent()) {
            cliente = clienteExistente.get();
            if (cliente.getPassword() != null && !cliente.getPassword().isBlank()) {
                return error(HttpStatus.CONFLICT, "Ya existe una cuenta para este documento.");
            }
            if (cliente.getCorreo() == null || !cliente.getCorreo().trim().equalsIgnoreCase(correo)) {
                return error(HttpStatus.CONFLICT,
                        "El correo no coincide con el registrado para este documento. Solicite actualizar sus datos en una agencia.");
            }
        } else {
            if (solicitud.nombres() == null || solicitud.nombres().isBlank()) {
                return error(HttpStatus.BAD_REQUEST, "Los nombres o razón social son obligatorios.");
            }
            if (documento.length() == 8 && (solicitud.apellidos() == null || solicitud.apellidos().isBlank())) {
                return error(HttpStatus.BAD_REQUEST, "Los apellidos son obligatorios para DNI.");
            }
            if (clienteRepository.existsByCorreoIgnoreCase(correo)) {
                return error(HttpStatus.CONFLICT, "El correo ya está asociado a otro cliente.");
            }

            cliente = new Cliente();
            cliente.setNumDocumento(documento);
            cliente.setNombres(solicitud.nombres().trim());
            cliente.setApellidos(solicitud.apellidos() == null ? "" : solicitud.apellidos().trim());
            cliente.setTelefono(telefono);
            cliente.setCorreo(correo);
        }

        cliente.setPassword(passwordEncoder.encode(password));
        clienteRepository.save(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Cuenta creada. Ya puedes iniciar sesión.",
                "numDocumento", cliente.getNumDocumento()));
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> iniciarSesion(
            @RequestBody PortalLoginSolicitud solicitud,
            HttpServletRequest request) {
        String documento = normalizar(solicitud.numDocumento());
        String password = solicitud.password() == null ? "" : solicitud.password();

        if (documento == null || password.isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "Documento o contraseña incorrectos.");
        }
        Optional<Cliente> encontrado = clienteRepository.findByNumDocumento(documento);
        if (encontrado.isEmpty() || !contrasenaValida(encontrado.get().getPassword(), password)) {
            return error(HttpStatus.UNAUTHORIZED, "Documento o contraseña incorrectos.");
        }

        Cliente cliente = encontrado.get();
        if (!esHashBcrypt(cliente.getPassword())) {
            cliente.setPassword(passwordEncoder.encode(password));
            clienteRepository.save(cliente);
        }

        HttpSession sesionActual = request.getSession(false);
        if (sesionActual != null) {
            sesionActual.invalidate();
        }
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute(SESION_CLIENTE_ID, cliente.getIdCliente());
        return ResponseEntity.ok(Map.of(
                "nombres", valorSeguro(cliente.getNombres()),
                "apellidos", valorSeguro(cliente.getApellidos())));
    }

    @GetMapping("/sesion")
    public ResponseEntity<?> sesion(HttpSession session) {
        Cliente cliente = clienteAutenticado(session);
        if (cliente == null) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return ResponseEntity.ok(Map.of(
                "nombres", valorSeguro(cliente.getNombres()),
                "apellidos", valorSeguro(cliente.getApellidos()),
                "numDocumento", valorSeguro(cliente.getNumDocumento())));
    }

    @PostMapping("/salir")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mis-encomiendas")
    public ResponseEntity<?> misEncomiendas(HttpSession session) {
        Cliente cliente = clienteAutenticado(session);
        if (cliente == null) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }

        List<PortalEncomiendaRespuesta> respuesta = encomiendaRepository
                .findByRemitente_IdClienteOrDestinatario_IdClienteOrderByIdEncomiendaDesc(
                        cliente.getIdCliente(), cliente.getIdCliente())
                .stream()
                .map(encomienda -> convertir(encomienda, cliente.getIdCliente()))
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    private PortalEncomiendaRespuesta convertir(Encomienda encomienda, Long clienteId) {
        List<EventoTrackingRespuesta> historial = eventoTrackingRepository
                .findByEncomienda_IdEncomiendaOrderByFechaAsc(encomienda.getIdEncomienda())
                .stream()
                .map(EventoTrackingRespuesta::desde)
                .toList();
        if (historial.isEmpty()) {
            historial = List.of(new EventoTrackingRespuesta(
                    encomienda.getEstadoLogistico(),
                    "Estado actual del envío. Los eventos anteriores a la activación del historial no están disponibles.",
                    null,
                    null));
        }
        Double pesoOriginal = encomienda.getPesoOriginal() == null
                ? encomienda.getPeso()
                : encomienda.getPesoOriginal();
        List<BultoEncomiendaRespuesta> bultos = new java.util.ArrayList<>();
        bultos.add(new BultoEncomiendaRespuesta(encomienda.getDescripcion(), pesoOriginal, true));
        bultos.addAll(bultoRepository
                .findByEncomienda_IdEncomiendaOrderByFechaRegistroAsc(encomienda.getIdEncomienda())
                .stream()
                .map(bulto -> new BultoEncomiendaRespuesta(bulto.getDescripcion(), bulto.getPeso(), false))
                .toList());
        return PortalEncomiendaRespuesta.desde(
                encomienda,
                clienteId,
                estimacionEntregaService.calcularFechaEstimada(encomienda).orElse(null),
                historial,
                bultos);
    }

    private Cliente clienteAutenticado(HttpSession session) {
        Object clienteId = session.getAttribute(SESION_CLIENTE_ID);
        if (!(clienteId instanceof Long id)) {
            return null;
        }
        return clienteRepository.findById(id).orElse(null);
    }

    private boolean esHashBcrypt(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private boolean contrasenaValida(String almacenada, String recibida) {
        if (almacenada == null) {
            return false;
        }
        return esHashBcrypt(almacenada)
                ? passwordEncoder.matches(recibida, almacenada)
                : almacenada.equals(recibida);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("mensaje", mensaje));
    }
}
