package com.perubusSystem.perubus_system.dto;

public record PortalRegistroSolicitud(
        String numDocumento,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        String password) {
}
