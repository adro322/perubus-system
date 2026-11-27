package com.perubusSystem.perubus_system.dto;

public record ClienteResumen(
        String nombres,
        String apellidos,
        String numDocumento,
        String telefono,
        String correo) {
}
