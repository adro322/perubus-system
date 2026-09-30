package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "encomienda")
@Getter
@Setter
public class Encomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEncomienda;

    @Column(length = 50, unique = true, nullable = false)
    private String codigoTracking;

    @Column(columnDefinition = "DECIMAL(10,2)", nullable = false)
    private Double peso;

    @Column(name = "estado", columnDefinition = "TEXT", nullable = false)
    private String estadoLogistico;

    // --- NUEVOS CAMPOS ---
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "DECIMAL(10,2)", nullable = false)
    private Double tarifaBase;

    // --- NUEVAS RELACIONES DE CLIENTE ---
    @ManyToOne
    @JoinColumn(name = "idRemitente", nullable = false)
    private Cliente remitente;

    @ManyToOne
    @JoinColumn(name = "idDestinatario", nullable = false)
    private Cliente destinatario;

    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    // Este NO lleva nullable = false porque al registrar la caja aún no hay bus
    @ManyToOne
    @JoinColumn(name = "idManifiesto")
    private Manifiesto manifiesto;

    public Encomienda() {
    }
}