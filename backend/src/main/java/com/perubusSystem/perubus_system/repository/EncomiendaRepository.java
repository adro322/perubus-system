package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Encomienda;
import org.springframework.data.jpa.repository.JpaRepository;

import com.perubusSystem.perubus_system.model.Manifiesto;
import java.util.List;
import java.util.Optional;

public interface EncomiendaRepository extends JpaRepository<Encomienda, Long> {

	Optional<Encomienda> findByCodigoTracking(String codigoTracking);

	List<Encomienda> findByManifiesto(Manifiesto manifiesto);
}