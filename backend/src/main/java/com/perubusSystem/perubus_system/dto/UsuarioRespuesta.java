package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Usuario;

// Datos públicos de un empleado: nunca incluye la contraseña
public record UsuarioRespuesta(
        Long idUsuario,
        String username,
        String rol,
        Boolean estado) {

    public static UsuarioRespuesta desde(Usuario u) {
        return new UsuarioRespuesta(u.getIdUsuario(), u.getUsername(), u.getRol(), u.getEstado());
    }
}
