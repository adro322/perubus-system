package com.perubusSystem.perubus_system.controller;

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
        return ResponseEntity.ok(clienteGuardado);
    }

    @GetMapping
    public ResponseEntity<?> listarClientes() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    @GetMapping("/buscar/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni) {
        return clienteRepository.findByNumDocumento(dni)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}