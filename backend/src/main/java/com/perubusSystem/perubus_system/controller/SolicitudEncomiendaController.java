package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.DecisionSolicitudEncomienda;
import com.perubusSystem.perubus_system.dto.SolicitudEncomiendaSolicitud;
import com.perubusSystem.perubus_system.service.SolicitudEncomiendaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class SolicitudEncomiendaController {

    private final SolicitudEncomiendaService solicitudService;

    public SolicitudEncomiendaController(SolicitudEncomiendaService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/api/portal/solicitudes")
    public ResponseEntity<?> solicitudesDelCliente(HttpSession session) {
        Object clienteId = session.getAttribute("portalClienteId");
        if (!(clienteId instanceof Long id)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión como cliente para consultar tus solicitudes.");
        }
        return ResponseEntity.ok(solicitudService.listarDelCliente(id));
    }

    @PostMapping("/api/portal/encomiendas/{codigoTracking}/solicitudes")
    public ResponseEntity<?> crearSolicitud(
            @PathVariable String codigoTracking,
            @RequestBody SolicitudEncomiendaSolicitud solicitud,
            HttpSession session) {
        Object clienteId = session.getAttribute("portalClienteId");
        if (!(clienteId instanceof Long id)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión como cliente para solicitar cambios.");
        }
        try {
            var respuesta = solicitudService.crear(
                    id, codigoTracking.trim().toUpperCase(Locale.ROOT), solicitud);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/api/portal/solicitudes/{id}/pagar")
    public ResponseEntity<?> pagarImporteAdicional(@PathVariable Long id, HttpSession session) {
        Object clienteId = session.getAttribute("portalClienteId");
        if (!(clienteId instanceof Long cliente)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión como cliente para pagar el importe adicional.");
        }
        try {
            return ResponseEntity.ok(solicitudService.pagarImporteAdicional(cliente, id));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/api/encomiendas/solicitudes-pendientes")
    public ResponseEntity<?> solicitudesPendientes(HttpSession session) {
        Object usuarioId = session.getAttribute("idUsuario");
        if (!(usuarioId instanceof Long id)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión como personal para revisar solicitudes.");
        }
        try {
            return ResponseEntity.ok(solicitudService.listarPendientes(id));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/api/encomiendas/solicitudes/{id}/resolver")
    public ResponseEntity<?> resolverSolicitud(
            @PathVariable Long id,
            @RequestBody DecisionSolicitudEncomienda decision,
            HttpSession session) {
        Object usuarioId = session.getAttribute("idUsuario");
        if (!(usuarioId instanceof Long usuario)) {
            return error(HttpStatus.UNAUTHORIZED, "Inicia sesión como personal para gestionar solicitudes.");
        }
        try {
            return ResponseEntity.ok(solicitudService.resolver(id, usuario, decision));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("mensaje", mensaje));
    }
}
