package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.NoSuchElementException;

// Gestión de las cuentas del portal desde el personal: generar o restablecer el acceso y bloquear.
@Service
public class CuentaPortalService {

    // Sin caracteres que se confundan al dictarlos (0/O, 1/l/I)
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_CLAVE = 10;

    private final SecureRandom azar = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private ClienteRepository clienteRepository;

    public record AccesoGenerado(Cliente cliente, String passwordTemporal, boolean restablecido) {
    }

    // Crea la cuenta o, si ya existía, restablece su clave. La clave temporal se devuelve una sola vez;
    // en la base solo queda cifrada, y el cliente debe cambiarla en su primer ingreso.
    @Transactional
    public AccesoGenerado generarAcceso(Long clienteId) {
        Cliente cliente = buscar(clienteId);
        if (cliente.cuentaBloqueada()) {
            throw new IllegalStateException(
                    "La cuenta del portal está bloqueada. Solo un administrador o supervisor puede desbloquearla.");
        }
        boolean restablecido = cliente.tieneCuentaPortal();
        String temporal = generarClave();
        cliente.setPassword(encoder.encode(temporal));
        cliente.setDebeCambiarPassword(true);
        clienteRepository.save(cliente);
        return new AccesoGenerado(cliente, temporal, restablecido);
    }

    @Transactional
    public Cliente cambiarBloqueo(Long clienteId, boolean bloqueado) {
        Cliente cliente = buscar(clienteId);
        if (bloqueado && !cliente.tieneCuentaPortal()) {
            throw new IllegalStateException("El cliente aún no tiene una cuenta del portal.");
        }
        cliente.setPortalBloqueado(bloqueado);
        return clienteRepository.save(cliente);
    }

    String generarClave() {
        StringBuilder clave = new StringBuilder(LONGITUD_CLAVE);
        for (int i = 0; i < LONGITUD_CLAVE; i++) {
            clave.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return clave.toString();
    }

    private Cliente buscar(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el cliente."));
    }
}
