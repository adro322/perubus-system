package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Spring Boot crea la consulta SQL automáticamente solo con leer el nombre del
    // método
    Optional<Usuario> findByUsernameAndPassword(String username, String password);

}