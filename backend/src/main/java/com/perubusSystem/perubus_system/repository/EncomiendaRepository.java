package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Encomienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncomiendaRepository extends JpaRepository<Encomienda, Long> {

	Optional<Encomienda> findByCodigoTracking(String codigoTracking);
}