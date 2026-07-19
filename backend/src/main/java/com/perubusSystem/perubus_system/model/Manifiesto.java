package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import java.util.Date;

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

    @Temporal(TemporalType.DATE)
    private Date fechaViaje;

    public Manifiesto() {
    }

    public Long getIdManifiesto() {
        return idManifiesto;
    }

    public void setIdManifiesto(Long idManifiesto) {
        this.idManifiesto = idManifiesto;
    }
    // Puedes omitir los demás getters/setters por ahora para ganar tiempo
}