package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "solicitud_encomienda")
@Getter
@Setter
public class SolicitudEncomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSolicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idEncomienda", nullable = false)
    private Encomienda encomienda;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @Column(length = 30, nullable = false)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String direccionSolicitada;

    @Column(length = 250)
    private String descripcionBulto;

    @Column(columnDefinition = "DECIMAL(10,2)")
    private Double pesoBulto;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoAdicional;

    private LocalDateTime fechaPagoAdicional;

    @Column(length = 20, nullable = false)
    private String estado;

    @Column(length = 500)
    private String respuesta;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    @PrePersist
    private void asignarFechaSolicitud() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
    }
}
