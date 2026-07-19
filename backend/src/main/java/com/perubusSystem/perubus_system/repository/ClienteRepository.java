package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Esto nos servirá para autocompletar datos si el cliente ya existe (CUS-01
    // Flujo Alternativo 2a)
    Optional<Cliente> findByNumDocumento(String numDocumento);
}