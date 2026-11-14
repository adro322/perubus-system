package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulto_encomienda")
@Getter
@Setter
public class BultoEncomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idBulto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idEncomienda", nullable = false)
    private Encomienda encomienda;

    @Column(length = 250, nullable = false)
    private String descripcion;

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double peso;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    private void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
