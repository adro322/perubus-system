package com.perubusSystem.perubus_system.Controller;

import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> autenticarUsuario(@RequestBody Usuario credenciales) {

        Optional<Usuario> usuarioBd = usuarioRepository.findByUsernameAndPassword(
                credenciales.getUsername(),
                credenciales.getPassword());

        if (usuarioBd.isPresent() && usuarioBd.get().getEstado()) {
            return ResponseEntity.ok(usuarioBd.get());
        } else {
            return ResponseEntity.status(401).body("Usuario o contraseña incorrectos");
        }
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        nuevoUsuario.setEstado(true); // Activo por defecto
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        return ResponseEntity.ok(usuarioGuardado);
    }
}