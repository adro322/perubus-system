package com.perubusSystem.perubus_system.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // 8 dígitos = DNI, 11 dígitos = RUC
    @Column(length = 11)
    private String numDocumento;

    @Column(length = 3)
    private String tipoDocumento;

    @Column(columnDefinition = "TEXT")
    private String nombres;

    @Column(columnDefinition = "TEXT")
    private String apellidos;

    @Column(length = 9)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String correo;

    @Column(length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @PrePersist
    @PreUpdate
    private void definirTipoDocumento() {
        if (numDocumento != null)
            tipoDocumento = numDocumento.length() == 11 ? "RUC" : "DNI";
    }

    public Cliente() {
    }
}