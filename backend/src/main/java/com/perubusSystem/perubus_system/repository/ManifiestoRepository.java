package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Manifiesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManifiestoRepository extends JpaRepository<Manifiesto, Long> {
    // Aquí podrías agregar búsquedas, ej: List<Manifiesto> findByRuta(String ruta);
}