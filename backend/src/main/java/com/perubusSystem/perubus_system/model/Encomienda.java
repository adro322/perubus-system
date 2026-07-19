package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "encomienda")
public class Encomienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEncomienda;

    @Column(length = 50, unique = true)
    private String codigoTracking;

    @Column(columnDefinition = "DECIMAL(10,2)")
    private Double peso;

    @Column(columnDefinition = "TEXT")
    private String estado;
    @ManyToOne
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "idManifiesto")
    private Manifiesto manifiesto;

    public Encomienda() {
    }

    // Getters y Setters
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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