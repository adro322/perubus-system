package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.*;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.BultoEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.seguridad.LimitadorIntentos;
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
    // Sesión a medias: el cliente entró con una clave temporal y aún debe cambiarla
    private static final String SESION_CAMBIO_PENDIENTE = "portalCambioPendienteId";
    private static final String MENSAJE_NO_VERIFICADO = "No pudimos verificar tus datos. Usa el mismo correo y teléfono "
            + "que diste en la agencia, o pide tus credenciales de acceso en una agencia PeruBus.";
    private static final String MENSAJE_BLOQUEADA = "Tu cuenta del portal está bloqueada. Comunícate con una agencia PeruBus.";
    private final LimitadorIntentos limitador = new LimitadorIntentos();
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

    // Activa la cuenta de un cliente YA registrado en una agencia. No crea clientes nuevos:
    // los nombres salen siempre de la base de datos, nunca de lo que escriba el visitante.
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
        if (!claveValida(password)) {
            return error(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres y no superar 72 bytes UTF-8.");
        }
        String telefono = normalizar(solicitud.telefono());
        if (telefono != null && !telefono.isBlank() && !telefono.matches("\\d{9}")) {
            return error(HttpStatus.BAD_REQUEST, "El teléfono debe tener 9 dígitos.");
        }

        String clave = "registro:" + documento;
        if (limitador.bloqueado(clave)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, mensajeLimite(clave));
        }

        // El mismo mensaje si el documento no existe o los datos no coinciden: no revela qué documentos están registrados
        Optional<Cliente> encontrado = clienteRepository.findByNumDocumento(documento);
        if (encontrado.isEmpty() || !datosCoinciden(encontrado.get(), correo, telefono)) {
            limitador.fallo(clave);
            return error(HttpStatus.CONFLICT, MENSAJE_NO_VERIFICADO);
        }

        Cliente cliente = encontrado.get();
        if (cliente.cuentaBloqueada()) {
            return error(HttpStatus.FORBIDDEN, MENSAJE_BLOQUEADA);
        }
        if (cliente.tieneCuentaPortal()) {
            return error(HttpStatus.CONFLICT, "Ya existe una cuenta para este documento. Inicia sesión con tu contraseña.");
        }

        cliente.setPassword(passwordEncoder.encode(password));
        cliente.setDebeCambiarPassword(false);
        clienteRepository.save(cliente);
        limitador.exito(clave);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Cuenta activada. Ya puedes iniciar sesión.",
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
        String clave = "portal:" + documento;
        if (limitador.bloqueado(clave)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, mensajeLimite(clave));
        }
        Optional<Cliente> encontrado = clienteRepository.findByNumDocumento(documento);
        if (encontrado.isEmpty() || !contrasenaValida(encontrado.get().getPassword(), password)) {
            limitador.fallo(clave);
            return error(HttpStatus.UNAUTHORIZED, "Documento o contraseña incorrectos.");
        }

        Cliente cliente = encontrado.get();
        // La contraseña ya es correcta: recién ahora se avisa que la cuenta está bloqueada
        if (cliente.cuentaBloqueada()) {
            return error(HttpStatus.FORBIDDEN, MENSAJE_BLOQUEADA);
        }
        limitador.exito(clave);
        if (!esHashBcrypt(cliente.getPassword())) {
            cliente.setPassword(passwordEncoder.encode(password));
            clienteRepository.save(cliente);
        }

        HttpSession sesionActual = request.getSession(false);
        if (sesionActual != null) {
            sesionActual.invalidate();
        }
        HttpSession sesion = request.getSession(true);
        boolean debeCambiar = cliente.debeCambiarClave();
        // Con clave temporal no se abre la sesión completa: solo se permite cambiar la clave
        sesion.setAttribute(debeCambiar ? SESION_CAMBIO_PENDIENTE : SESION_CLIENTE_ID, cliente.getIdCliente());
        return ResponseEntity.ok(Map.of(
                "debeCambiarPassword", debeCambiar,
                "nombres", valorSeguro(cliente.getNombres()),
                "apellidos", valorSeguro(cliente.getApellidos())));
    }

    // Cambio de clave: obligatorio tras una clave temporal y voluntario con la sesión abierta
    @PostMapping("/cambiar-password")
    @Transactional
    public ResponseEntity<?> cambiarPassword(
            @RequestBody PortalCambioClaveSolicitud solicitud,
            HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object id = sesion == null ? null : sesion.getAttribute(SESION_CAMBIO_PENDIENTE);
        if (id == null && sesion != null) {
            id = sesion.getAttribute(SESION_CLIENTE_ID);
        }
        if (!(id instanceof Long clienteId)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null || cliente.cuentaBloqueada()) {
            sesion.invalidate();
            return error(HttpStatus.FORBIDDEN, MENSAJE_BLOQUEADA);
        }

        String actual = solicitud.passwordActual() == null ? "" : solicitud.passwordActual();
        String nueva = solicitud.passwordNueva() == null ? "" : solicitud.passwordNueva();
        String clave = "cambio:" + clienteId;
        if (limitador.bloqueado(clave)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, mensajeLimite(clave));
        }
        if (!contrasenaValida(cliente.getPassword(), actual)) {
            limitador.fallo(clave);
            return error(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta.");
        }
        if (!claveValida(nueva)) {
            return error(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe tener al menos 8 caracteres y no superar 72 bytes UTF-8.");
        }
        if (nueva.equals(actual)) {
            return error(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual.");
        }

        limitador.exito(clave);
        cliente.setPassword(passwordEncoder.encode(nueva));
        cliente.setDebeCambiarPassword(false);
        clienteRepository.save(cliente);
        sesion.removeAttribute(SESION_CAMBIO_PENDIENTE);
        sesion.setAttribute(SESION_CLIENTE_ID, clienteId);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Contraseña actualizada.",
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
        return clienteRepository.findById(id).filter(c -> !c.cuentaBloqueada()).orElse(null);
    }

    // El correo debe coincidir siempre; el teléfono, solo si el cliente dejó uno en la agencia
    private boolean datosCoinciden(Cliente cliente, String correo, String telefono) {
        if (cliente.getCorreo() == null || !cliente.getCorreo().trim().equalsIgnoreCase(correo)) {
            return false;
        }
        String registrado = cliente.getTelefono() == null ? "" : cliente.getTelefono().trim();
        return registrado.isEmpty() || registrado.equals(telefono == null ? "" : telefono);
    }

    private boolean claveValida(String clave) {
        return clave.length() >= 8 && clave.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private String mensajeLimite(String clave) {
        long minutos = Math.max(1, limitador.minutosRestantes(clave));
        return "Demasiados intentos fallidos. Inténtalo de nuevo en " + minutos + " minuto(s).";
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
