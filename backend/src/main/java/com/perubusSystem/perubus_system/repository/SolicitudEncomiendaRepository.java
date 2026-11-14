package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.SolicitudEncomienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudEncomiendaRepository extends JpaRepository<SolicitudEncomienda, Long> {
    boolean existsByEncomienda_IdEncomiendaAndEstado(Long encomiendaId, String estado);

    boolean existsByEncomienda_IdEncomiendaAndEstadoIn(Long encomiendaId, List<String> estados);

    List<SolicitudEncomienda> findByCliente_IdClienteOrderByFechaSolicitudDesc(Long clienteId);

    List<SolicitudEncomienda> findByEstadoOrderByFechaSolicitudAsc(String estado);

    List<SolicitudEncomienda> findByTipoOrderByFechaSolicitudDesc(String tipo);
}
