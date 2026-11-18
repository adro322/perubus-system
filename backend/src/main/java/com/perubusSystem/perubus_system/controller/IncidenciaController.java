package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.IncidenciaResolucionSolicitud;
import com.perubusSystem.perubus_system.dto.IncidenciaRespuesta;
import com.perubusSystem.perubus_system.dto.IncidenciaSolicitud;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.EventoTracking;
import com.perubusSystem.perubus_system.model.Incidencia;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.repository.IncidenciaRepository;
import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.seguridad.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RequiereRol({ Rol.AGENTE, Rol.OPERARIO })
@RestController
@RequestMapping("/api/incidencias")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class IncidenciaController {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private EncomiendaRepository encomiendaRepository;

    @Autowired
    private EventoTrackingRepository eventoTrackingRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<IncidenciaRespuesta> listarIncidencias() {
        return incidenciaRepository.findAllByOrderByFechaReporteDesc().stream()
                .map(IncidenciaRespuesta::desde)
                .toList();
    }

    @GetMapping("/encomienda/{codigoTracking}")
    @Transactional(readOnly = true)
    public List<IncidenciaRespuesta> listarPorEncomienda(@PathVariable String codigoTracking) {
        String tracking = codigoTracking == null ? "" : codigoTracking.trim().toUpperCase(Locale.ROOT);
        return incidenciaRepository.findByEncomienda_CodigoTrackingOrderByFechaReporteDesc(tracking).stream()
                .map(IncidenciaRespuesta::desde)
                .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> reportarIncidencia(@RequestBody IncidenciaSolicitud solicitud) {
        if (solicitud == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Datos de incidencia inválidos."));
        }
        String tracking = solicitud.codigoTracking() == null
                ? ""
                : solicitud.codigoTracking().trim().toUpperCase(Locale.ROOT);
        if (!tracking.matches("PERU-[A-Z0-9]{8}")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Ingresa un código de tracking válido (PERU-XXXXXXXX)."));
        }
        String tipo = solicitud.tipoProblema() == null ? "" : solicitud.tipoProblema().trim();
        if (tipo.isEmpty() || tipo.length() > 100) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Selecciona un tipo de problema válido (máximo 100 caracteres)."));
        }
        String descripcion = solicitud.descripcion() == null ? "" : solicitud.descripcion().trim();
        if (descripcion.isEmpty() || descripcion.length() > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "La descripción del problema es obligatoria (máximo 1000 caracteres)."));
        }

        Optional<Encomienda> encOpt = encomiendaRepository.findByCodigoTracking(tracking);
        if (encOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No existe una encomienda con el código " + tracking + "."));
        }
        Encomienda encomienda = encOpt.get();

        Incidencia incidencia = new Incidencia();
        incidencia.setEncomienda(encomienda);
        incidencia.setTipoProblema(tipo);
        incidencia.setDescripcion(descripcion);
        incidencia.setEstado("ABIERTA");
        incidencia.setFechaReporte(LocalDateTime.now());
        Incidencia guardada = incidenciaRepository.save(incidencia);

        EventoTracking evento = new EventoTracking();
        evento.setEncomienda(encomienda);
        evento.setEstado(encomienda.getEstadoLogistico());
        evento.setDetalle("Incidencia reportada (" + tipo + "): " + descripcion);
        eventoTrackingRepository.save(evento);

        return ResponseEntity.status(HttpStatus.CREATED).body(IncidenciaRespuesta.desde(guardada));
    }

    @PostMapping("/{id}/resolver")
    @Transactional
    public ResponseEntity<?> resolverIncidencia(
            @PathVariable Long id,
            @RequestBody(required = false) IncidenciaResolucionSolicitud solicitud) {
        Optional<Incidencia> incOpt = incidenciaRepository.findById(id);
        if (incOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró la incidencia."));
        }
        Incidencia incidencia = incOpt.get();
        if ("RESUELTA".equalsIgnoreCase(incidencia.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "La incidencia ya se encuentra resuelta."));
        }

        String resolucion = solicitud == null || solicitud.resolucion() == null
                ? ""
                : solicitud.resolucion().trim();
        if (resolucion.isEmpty() || resolucion.length() > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Indica el detalle de la solución aplicada (máximo 1000 caracteres)."));
        }

        incidencia.setEstado("RESUELTA");
        incidencia.setResolucion(resolucion);
        incidencia.setFechaResolucion(LocalDateTime.now());
        Incidencia guardada = incidenciaRepository.save(incidencia);

        if (incidencia.getEncomienda() != null) {
            Encomienda encomienda = incidencia.getEncomienda();
            EventoTracking evento = new EventoTracking();
            evento.setEncomienda(encomienda);
            evento.setEstado(encomienda.getEstadoLogistico());
            evento.setDetalle("Incidencia resuelta (" + incidencia.getTipoProblema() + "): " + resolucion);
            eventoTrackingRepository.save(evento);
        }

        return ResponseEntity.ok(IncidenciaRespuesta.desde(guardada));
    }
}