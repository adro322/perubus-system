package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.EventoTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoTrackingRepository extends JpaRepository<EventoTracking, Long> {
    List<EventoTracking> findByEncomienda_IdEncomiendaOrderByFechaAsc(Long encomiendaId);
}
