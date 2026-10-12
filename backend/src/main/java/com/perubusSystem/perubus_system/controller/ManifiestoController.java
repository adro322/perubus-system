package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ManifiestoSolicitud;
import com.perubusSystem.perubus_system.service.ManifiestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/manifiestos")
@CrossOrigin(origins = "*")
public class ManifiestoController {

    @Autowired
    private ManifiestoService manifiestoService;

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ManifiestoSolicitud datos) {
        try {
            var m = manifiestoService.crear(datos);
            return ResponseEntity.ok(manifiestoService.obtener(m.getIdManifiesto()));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(manifiestoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(manifiestoService.obtener(id));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    // Body: { "codigosTracking": ["PERU-XXXXXXXX", ...] }
    @PostMapping("/{id}/encomiendas")
    public ResponseEntity<?> asignar(@PathVariable Long id, @RequestBody Map<String, List<String>> cuerpo) {
        try {
            return ResponseEntity.ok(manifiestoService.asignarEncomiendas(id, cuerpo.get("codigosTracking")));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e);
        }
    }

    private ResponseEntity<?> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(Map.of("mensaje", e.getMessage()));
    }
}
