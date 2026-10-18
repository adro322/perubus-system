package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ClienteRespuesta;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*") // Permiso para que React se conecte
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarCliente(@RequestBody Cliente cliente) {
        Cliente clienteGuardado = clienteRepository.save(cliente);
        return ResponseEntity.ok(ClienteRespuesta.desde(clienteGuardado));
    }

    @GetMapping
    public ResponseEntity<?> listarClientes() {
        return ResponseEntity.ok(clienteRepository.findAll().stream()
                .map(ClienteRespuesta::desde)
                .toList());
    }

    @GetMapping("/buscar/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni) {
        if (dni == null || !dni.matches("\\d{8}|\\d{11}")) {
            return ResponseEntity.badRequest().body("El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC).");
        }
        return clienteRepository.findByNumDocumento(dni)
                .map(c -> ResponseEntity.ok(ClienteRespuesta.desde(c)))
                .orElse(ResponseEntity.notFound().build());
    }
}