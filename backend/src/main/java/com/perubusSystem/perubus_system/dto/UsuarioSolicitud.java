package com.perubusSystem.perubus_system.dto;

// Alta o edición de personal. En la edición, password se ignora (no se cambia).
public record UsuarioSolicitud(
        String username,
        String password,
        String rol,
        Boolean estado) {
}
