package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "encomienda")
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

    // --- Getters y Setters ---

    public Long getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(Long idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getCodigoTracking() {
        return codigoTracking;
    }

    public void setCodigoTracking(String codigoTracking) {
        this.codigoTracking = codigoTracking;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public String getEstadoLogistico() {
        return estadoLogistico;
    }

    public void setEstadoLogistico(String estadoLogistico) {
        this.estadoLogistico = estadoLogistico;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getTarifaBase() {
        return tarifaBase;
    }

    public void setTarifaBase(Double tarifaBase) {
        this.tarifaBase = tarifaBase;
    }

    public Cliente getRemitente() {
        return remitente;
    }

    public void setRemitente(Cliente remitente) {
        this.remitente = remitente;
    }

    public Cliente getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Cliente destinatario) {
        this.destinatario = destinatario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Manifiesto getManifiesto() {
        return manifiesto;
    }

    public void setManifiesto(Manifiesto manifiesto) {
        this.manifiesto = manifiesto;
    }
}