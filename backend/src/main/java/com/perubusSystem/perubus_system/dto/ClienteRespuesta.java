package com.perubusSystem.perubus_system.dto;

import com.perubusSystem.perubus_system.model.Cliente;

// Versión pública del cliente: nunca incluye el password.
// cuentaPortal: SIN_CUENTA, CLAVE_TEMPORAL (aún no la cambió), ACTIVA o BLOQUEADA
public record ClienteRespuesta(
        Long idCliente,
        String numDocumento,
        String tipoDocumento,
        String nombres,
        String apellidos,
        String telefono,
        String correo,
        String cuentaPortal) {

    public static ClienteRespuesta desde(Cliente c) {
        return new ClienteRespuesta(c.getIdCliente(), c.getNumDocumento(), c.getTipoDocumento(), c.getNombres(),
                c.getApellidos(), c.getTelefono(), c.getCorreo(), estadoCuenta(c));
    }

    public static String estadoCuenta(Cliente c) {
        if (!c.tieneCuentaPortal()) return "SIN_CUENTA";
        if (c.cuentaBloqueada()) return "BLOQUEADA";
        if (c.debeCambiarClave()) return "CLAVE_TEMPORAL";
        return "ACTIVA";
    }
}
