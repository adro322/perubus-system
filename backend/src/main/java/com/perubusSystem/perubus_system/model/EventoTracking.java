package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_tracking")
@Getter
@Setter
public class EventoTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEvento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idEncomienda", nullable = false)
    private Encomienda encomienda;

    @Column(length = 60, nullable = false)
    private String estado;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String detalle;

    @Column(length = 120)
    private String ubicacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    private void asignarFecha() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
