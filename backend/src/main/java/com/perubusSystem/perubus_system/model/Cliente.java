package com.perubusSystem.perubus_system.model;

import com.perubusSystem.perubus_system.util.NombrePersona;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // Cuenta del portal: la bloquea un administrador o supervisor
    @JsonIgnore
    @Column(name = "portal_bloqueado", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean portalBloqueado;

    // Verdadero cuando la clave la generó el personal: el cliente debe cambiarla en su primer ingreso
    @JsonIgnore
    @Column(name = "debe_cambiar_password", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean debeCambiarPassword;

    public boolean tieneCuentaPortal() {
        return password != null && !password.isBlank();
    }

    public boolean cuentaBloqueada() {
        return Boolean.TRUE.equals(portalBloqueado);
    }

    public boolean debeCambiarClave() {
        return Boolean.TRUE.equals(debeCambiarPassword);
    }

    @PrePersist
    @PreUpdate
    private void definirTipoDocumento() {
        if (numDocumento != null) {
            tipoDocumento = numDocumento.length() == 11 ? "RUC" : "DNI";
            if (numDocumento.length() == 8) {
                nombres = NombrePersona.formatoTitulo(nombres);
                apellidos = NombrePersona.formatoTitulo(apellidos);
            }
        }
    }

    public Cliente() {
    }
}