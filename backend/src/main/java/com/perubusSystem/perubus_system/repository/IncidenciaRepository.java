package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findAllByOrderByFechaReporteDesc();
    List<Incidencia> findByEncomienda_IdEncomiendaOrderByFechaReporteDesc(Long idEncomienda);
    List<Incidencia> findByEncomienda_CodigoTrackingOrderByFechaReporteDesc(String codigoTracking);
}