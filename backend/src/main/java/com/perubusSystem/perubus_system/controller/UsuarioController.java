package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.UsuarioRespuesta;
import com.perubusSystem.perubus_system.dto.UsuarioSolicitud;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import com.perubusSystem.perubus_system.seguridad.LimitadorIntentos;
import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.seguridad.Rol;
import com.perubusSystem.perubus_system.seguridad.SesionPersonal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SesionPersonal sesionPersonal;

    private final LimitadorIntentos limitador = new LimitadorIntentos();

    @PostMapping("/login")
    public ResponseEntity<?> autenticarUsuario(@RequestBody Usuario credenciales, HttpServletRequest request) {
        String clave = "personal:" + (credenciales.getUsername() == null ? "" : credenciales.getUsername().trim().toLowerCase());
        if (limitador.bloqueado(clave)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Inténtalo de nuevo en " + Math.max(1, limitador.minutosRestantes(clave)) + " minuto(s).");
        }

        Optional<Usuario> usuarioBd = usuarioRepository.findByUsernameAndPassword(
                credenciales.getUsername(),
                credenciales.getPassword());

        if (usuarioBd.isPresent() && usuarioBd.get().getEstado()) {
            HttpSession sesionActual = request.getSession(false);
            if (sesionActual != null) {
                sesionActual.invalidate();
            }
            request.getSession(true).setAttribute("idUsuario", usuarioBd.get().getIdUsuario());
            limitador.exito(clave);
            return ResponseEntity.ok(usuarioBd.get());
        } else {
            limitador.fallo(clave);
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

    // Solo ADMIN/SUPERVISOR crean personal. Única excepción: si aún no existe ningún usuario,
    // se permite crear el primero (siempre como ADMIN) para poder arrancar el sistema.
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarUsuario(@RequestBody UsuarioSolicitud datos, HttpServletRequest request) {
        boolean primerUsuario = usuarioRepository.count() == 0;
        if (!primerUsuario) {
            Optional<Usuario> actual = sesionPersonal.actual(request);
            if (actual.isEmpty()) {
                return mensaje(HttpStatus.UNAUTHORIZED, "Inicia sesión como administrador para registrar personal.");
            }
            if (!Rol.esSuperusuario(actual.get().getRol())) {
                return mensaje(HttpStatus.FORBIDDEN, "Solo un administrador o supervisor puede registrar personal.");
            }
        }

        String username = datos.username() == null ? "" : datos.username().trim();
        if (username.isEmpty())
            return mensaje(HttpStatus.BAD_REQUEST, "El usuario es obligatorio.");
        if (datos.password() == null || datos.password().length() < 6)
            return mensaje(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres.");
        Optional<Rol> rol = primerUsuario ? Optional.of(Rol.ADMIN) : Rol.desde(datos.rol());
        if (rol.isEmpty())
            return mensaje(HttpStatus.BAD_REQUEST, "El rol debe ser ADMIN, SUPERVISOR, AGENTE u OPERARIO.");
        if (usuarioRepository.existsByUsernameIgnoreCase(username))
            return mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");

        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPassword(datos.password());
        nuevo.setRol(rol.get().name());
        nuevo.setEstado(true); // Activo por defecto
        return ResponseEntity.ok(UsuarioRespuesta.desde(usuarioRepository.save(nuevo)));
    }

    @RequiereRol({})
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(usuarioRepository.findAll().stream().map(UsuarioRespuesta::desde).toList());
    }

    @RequiereRol({})
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody UsuarioSolicitud datos,
                                        HttpServletRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findById(id);
        if (encontrado.isEmpty())
            return mensaje(HttpStatus.NOT_FOUND, "No se encontró el usuario.");
        Usuario usuario = encontrado.get();

        String username = datos.username() == null ? usuario.getUsername() : datos.username().trim();
        if (username.isEmpty())
            return mensaje(HttpStatus.BAD_REQUEST, "El usuario es obligatorio.");
        Optional<Rol> rol = datos.rol() == null ? Rol.desde(usuario.getRol()) : Rol.desde(datos.rol());
        if (rol.isEmpty())
            return mensaje(HttpStatus.BAD_REQUEST, "El rol debe ser ADMIN, SUPERVISOR, AGENTE u OPERARIO.");
        boolean estado = datos.estado() == null ? Boolean.TRUE.equals(usuario.getEstado()) : datos.estado();
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdUsuarioNot(username, id))
            return mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");

        boolean sigueSiendoSuperusuarioActivo = rol.get().esSuperusuario() && estado;
        if (!sigueSiendoSuperusuarioActivo && esSuperusuarioActivo(usuario)) {
            String problema = protegerSuperusuario(usuario, request, "cambiar el rol o desactivar");
            if (problema != null)
                return mensaje(HttpStatus.CONFLICT, problema);
        }

        usuario.setUsername(username);
        usuario.setRol(rol.get().name());
        usuario.setEstado(estado);
        return ResponseEntity.ok(UsuarioRespuesta.desde(usuarioRepository.save(usuario)));
    }

    @RequiereRol({})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, HttpServletRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findById(id);
        if (encontrado.isEmpty())
            return mensaje(HttpStatus.NOT_FOUND, "No se encontró el usuario.");
        Usuario usuario = encontrado.get();

        if (esSuperusuarioActivo(usuario)) {
            String problema = protegerSuperusuario(usuario, request, "eliminar");
            if (problema != null)
                return mensaje(HttpStatus.CONFLICT, problema);
        }
        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return mensaje(HttpStatus.CONFLICT,
                    "El usuario tiene encomiendas registradas y no se puede eliminar. Desactívelo en su lugar.");
        }
    }

    private boolean esSuperusuarioActivo(Usuario u) {
        return Boolean.TRUE.equals(u.getEstado()) && Rol.esSuperusuario(u.getRol());
    }

    // Evita quedarse sin acceso total: no se puede quitar a uno mismo ni al último ADMIN/SUPERVISOR activo
    private String protegerSuperusuario(Usuario objetivo, HttpServletRequest request, String accion) {
        Optional<Usuario> actual = sesionPersonal.actual(request);
        if (actual.isPresent() && actual.get().getIdUsuario().equals(objetivo.getIdUsuario()))
            return "No puedes " + accion + " tu propio usuario.";
        long activos = usuarioRepository.findAll().stream().filter(this::esSuperusuarioActivo).count();
        if (activos <= 1)
            return "No se puede " + accion + " al último administrador o supervisor activo.";
        return null;
    }

    private ResponseEntity<?> mensaje(HttpStatus estado, String texto) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", texto));
    }
}
