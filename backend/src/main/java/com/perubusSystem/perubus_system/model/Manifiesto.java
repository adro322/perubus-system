package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "manifiesto")
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

    public Manifiesto() {
    }

    // --- Getters y Setters ---
    public Long getIdManifiesto() {
        return idManifiesto;
    }

    public void setIdManifiesto(Long idManifiesto) {
        this.idManifiesto = idManifiesto;
    }

    public String getNumBus() {
        return numBus;
    }

    public void setNumBus(String numBus) {
        this.numBus = numBus;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public LocalDate getFechaViaje() {
        return fechaViaje;
    }

    public void setFechaViaje(LocalDate fechaViaje) {
        this.fechaViaje = fechaViaje;
    }

    public Double getPesoTotalCarga() {
        return pesoTotalCarga;
    }

    public void setPesoTotalCarga(Double pesoTotalCarga) {
        this.pesoTotalCarga = pesoTotalCarga;
    }
}