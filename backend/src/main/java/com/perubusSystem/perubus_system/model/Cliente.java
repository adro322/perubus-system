package com.perubusSystem.perubus_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCliente;

    @Column(length = 8)
    private String numDocumento;

    @Column(columnDefinition = "TEXT")
    private String nombres;

    @Column(columnDefinition = "TEXT")
    private String apellidos;

    @Column(length = 9)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String correo;

    @Column(length = 255)
    private String password;

    public Cliente() {
    }
}