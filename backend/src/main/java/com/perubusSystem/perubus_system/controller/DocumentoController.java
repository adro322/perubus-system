package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    // 8 dígitos = DNI, 11 dígitos = RUC
    @GetMapping("/{numero}")
    public ResponseEntity<?> consultar(@PathVariable String numero) {
        try {
            return ResponseEntity.ok(documentoService.consultar(numero));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalStateException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, e);
        }
    }

    private ResponseEntity<?> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(Map.of("mensaje", e.getMessage()));
    }
}
