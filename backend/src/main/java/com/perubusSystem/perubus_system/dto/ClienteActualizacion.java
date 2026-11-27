package com.perubusSystem.perubus_system.dto;

// El documento no se modifica: identifica al cliente
public record ClienteActualizacion(
        String nombres,
        String apellidos,
        String telefono,
        String correo) {
}
