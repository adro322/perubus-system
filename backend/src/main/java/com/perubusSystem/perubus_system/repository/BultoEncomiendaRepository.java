package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.BultoEncomienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BultoEncomiendaRepository extends JpaRepository<BultoEncomienda, Long> {
    List<BultoEncomienda> findByEncomienda_IdEncomiendaOrderByFechaRegistroAsc(Long encomiendaId);
}
