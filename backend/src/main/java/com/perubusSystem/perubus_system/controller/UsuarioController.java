package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> autenticarUsuario(@RequestBody Usuario credenciales, HttpServletRequest request) {

        Optional<Usuario> usuarioBd = usuarioRepository.findByUsernameAndPassword(
                credenciales.getUsername(),
                credenciales.getPassword());

        if (usuarioBd.isPresent() && usuarioBd.get().getEstado()) {
            HttpSession sesionActual = request.getSession(false);
            if (sesionActual != null) {
                sesionActual.invalidate();
            }
            request.getSession(true).setAttribute("idUsuario", usuarioBd.get().getIdUsuario());
            return ResponseEntity.ok(usuarioBd.get());
        } else {
            return ResponseEntity.status(401).body("Usuario o contraseña incorrectos");
        }
    }

    @PostMapping("/salir")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        nuevoUsuario.setEstado(true); // Activo por defecto
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        return ResponseEntity.ok(usuarioGuardado);
    }
}