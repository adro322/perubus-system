package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.seguridad.Rol;
import com.perubusSystem.perubus_system.dto.ClienteActualizacion;
import com.perubusSystem.perubus_system.dto.ClienteRespuesta;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.service.CuentaPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaPortalService cuentaPortalService;

    @RequiereRol(Rol.AGENTE)
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarCliente(@RequestBody Cliente cliente) {
        String documento = cliente.getNumDocumento() == null ? "" : cliente.getNumDocumento().trim();
        if (!documento.matches("\\d{8}|\\d{11}"))
            return mensaje(HttpStatus.BAD_REQUEST, "El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC).");
        if (clienteRepository.findByNumDocumento(documento).isPresent())
            return mensaje(HttpStatus.CONFLICT, "Ya existe un cliente con ese documento.");
        cliente.setNumDocumento(documento);
        // La cuenta del portal solo se crea con "Generar acceso"
        cliente.setPassword(null);
        cliente.setPortalBloqueado(false);
        cliente.setDebeCambiarPassword(false);
        Cliente clienteGuardado = clienteRepository.save(cliente);
        return ResponseEntity.ok(ClienteRespuesta.desde(clienteGuardado));
    }

    @RequiereRol({})
    @GetMapping
    public ResponseEntity<?> listarClientes() {
        return ResponseEntity.ok(clienteRepository.findAll().stream()
                .map(ClienteRespuesta::desde)
                .toList());
    }

    @RequiereRol(Rol.AGENTE)
    @GetMapping("/buscar/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni) {
        if (dni == null || !dni.matches("\\d{8}|\\d{11}")) {
            return ResponseEntity.badRequest().body("El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC).");
        }
        return clienteRepository.findByNumDocumento(dni)
                .map(c -> ResponseEntity.ok(ClienteRespuesta.desde(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @RequiereRol({})
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCliente(@PathVariable Long id, @RequestBody ClienteActualizacion datos) {
        var encontrado = clienteRepository.findById(id);
        if (encontrado.isEmpty())
            return mensaje(HttpStatus.NOT_FOUND, "No se encontró el cliente.");
        Cliente cliente = encontrado.get();

        String nombres = datos.nombres() == null ? "" : datos.nombres().trim();
        if (nombres.isEmpty())
            return mensaje(HttpStatus.BAD_REQUEST, "Los nombres o razón social son obligatorios.");
        String telefono = datos.telefono() == null ? "" : datos.telefono().trim();
        if (!telefono.isEmpty() && !telefono.matches("\\d{9}"))
            return mensaje(HttpStatus.BAD_REQUEST, "El teléfono debe tener 9 dígitos.");
        String correo = datos.correo() == null ? "" : datos.correo().trim().toLowerCase();
        if (!correo.isEmpty() && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            return mensaje(HttpStatus.BAD_REQUEST, "Ingrese un correo electrónico válido.");
        if (!correo.isEmpty() && !correo.equalsIgnoreCase(cliente.getCorreo())
                && clienteRepository.existsByCorreoIgnoreCase(correo))
            return mensaje(HttpStatus.CONFLICT, "El correo ya está asociado a otro cliente.");

        cliente.setNombres(nombres);
        cliente.setApellidos(datos.apellidos() == null ? "" : datos.apellidos().trim());
        cliente.setTelefono(telefono.isEmpty() ? null : telefono);
        cliente.setCorreo(correo.isEmpty() ? null : correo);
        return ResponseEntity.ok(ClienteRespuesta.desde(clienteRepository.save(cliente)));
    }

    @RequiereRol({})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        if (!clienteRepository.existsById(id))
            return mensaje(HttpStatus.NOT_FOUND, "No se encontró el cliente.");
        try {
            clienteRepository.deleteById(id);
            clienteRepository.flush();
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return mensaje(HttpStatus.CONFLICT,
                    "El cliente tiene encomiendas o solicitudes asociadas y no se puede eliminar.");
        }
    }

    // Genera la cuenta del portal del cliente o restablece su clave. La clave temporal se muestra una sola vez.
    @RequiereRol(Rol.AGENTE)
    @PostMapping("/{id}/acceso")
    public ResponseEntity<?> generarAcceso(@PathVariable Long id) {
        try {
            var acceso = cuentaPortalService.generarAcceso(id);
            Cliente cliente = acceso.cliente();
            return ResponseEntity.ok(Map.of(
                    "idCliente", cliente.getIdCliente(),
                    "numDocumento", cliente.getNumDocumento(),
                    "nombre", (cliente.getNombres() + " " + (cliente.getApellidos() == null ? "" : cliente.getApellidos())).trim(),
                    "passwordTemporal", acceso.passwordTemporal(),
                    "accion", acceso.restablecido() ? "RESTABLECIDO" : "GENERADO"));
        } catch (NoSuchElementException e) {
            return mensaje(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return mensaje(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    // Solo ADMIN y SUPERVISOR: Map con { "bloqueado": true | false }
    @RequiereRol({})
    @PutMapping("/{id}/bloqueo")
    public ResponseEntity<?> cambiarBloqueo(@PathVariable Long id, @RequestBody Map<String, Boolean> cuerpo) {
        if (cuerpo == null || cuerpo.get("bloqueado") == null)
            return mensaje(HttpStatus.BAD_REQUEST, "Indique si la cuenta debe quedar bloqueada.");
        try {
            return ResponseEntity.ok(ClienteRespuesta.desde(cuentaPortalService.cambiarBloqueo(id, cuerpo.get("bloqueado"))));
        } catch (NoSuchElementException e) {
            return mensaje(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return mensaje(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private ResponseEntity<?> mensaje(HttpStatus estado, String texto) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", texto));
    }
}
