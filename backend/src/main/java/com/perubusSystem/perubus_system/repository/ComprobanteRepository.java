package com.perubusSystem.perubus_system.repository;

import com.perubusSystem.perubus_system.model.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    Optional<Comprobante> findByEncomienda_CodigoTracking(String codigoTracking);
    Optional<Comprobante> findByEncomienda_IdEncomienda(Long idEncomienda);
    List<Comprobante> findAllByOrderByFechaPagoDesc();
}