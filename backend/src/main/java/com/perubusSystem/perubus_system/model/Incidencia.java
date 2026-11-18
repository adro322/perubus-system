package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia")
@Getter
@Setter
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia")
    private Long idIncidencia;

    @Column(name = "tipo_problema", length = 100)
    private String tipoProblema;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "estado", length = 30)
    private String estado;

    @Column(name = "resolucion", columnDefinition = "TEXT")
    private String resolucion;

    @Column(name = "fecha_reporte")
    private LocalDateTime fechaReporte;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @ManyToOne
    @JoinColumn(name = "id_encomienda")
    private Encomienda encomienda;

    @PrePersist
    private void inicializar() {
        if (fechaReporte == null) {
            fechaReporte = LocalDateTime.now();
        }
        if (estado == null || estado.isBlank()) {
            estado = "ABIERTA";
        }
    }

    public Incidencia() {
    }
}