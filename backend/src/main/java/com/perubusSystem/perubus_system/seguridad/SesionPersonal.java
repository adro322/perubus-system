package com.perubusSystem.perubus_system.seguridad;

import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Obtiene el empleado de la sesión consultando la base de datos en cada petición,
// así un cambio de rol o una desactivación se aplican de inmediato.
@Component
public class SesionPersonal {

    public static final String ATRIBUTO_USUARIO = "usuarioActual";

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Optional<Usuario> actual(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute("idUsuario") instanceof Long id)) {
            return Optional.empty();
        }
        return usuarioRepository.findById(id).filter(u -> Boolean.TRUE.equals(u.getEstado()));
    }
}
