package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "manifiesto")
@Getter
@Setter
public class Manifiesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idManifiesto;

    @Column(length = 20)
    private String numBus;

    @Column(columnDefinition = "TEXT")
    private String ruta;

    private LocalDate fechaViaje;

    // --- NUEVO CAMPO ---
    @Column(columnDefinition = "DECIMAL(10,2)")
    private Double pesoTotalCarga;

    // Capacidad de la bodega del bus (RF-07: validar peso total vs capacidad)
    @Column(columnDefinition = "DECIMAL(10,2)")
    private Double capacidadKg;

    public Manifiesto() {
    }
}