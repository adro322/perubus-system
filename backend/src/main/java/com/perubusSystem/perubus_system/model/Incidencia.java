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

    @Column(name = "fecha_reporte")
    private LocalDateTime fechaReporte;

    @ManyToOne
    @JoinColumn(name = "id_encomienda")
    private Encomienda encomienda;

    public Incidencia() {
    }
}