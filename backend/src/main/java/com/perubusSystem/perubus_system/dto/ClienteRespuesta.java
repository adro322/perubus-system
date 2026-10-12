package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Cliente;

// Versión pública del cliente: nunca incluye el password
public record ClienteRespuesta(
        Long idCliente,
        String numDocumento,
        String nombres,
        String apellidos,
        String telefono,
        String correo) {

    public static ClienteRespuesta desde(Cliente c) {
        return new ClienteRespuesta(c.getIdCliente(), c.getNumDocumento(), c.getNombres(),
                c.getApellidos(), c.getTelefono(), c.getCorreo());
    }
}
