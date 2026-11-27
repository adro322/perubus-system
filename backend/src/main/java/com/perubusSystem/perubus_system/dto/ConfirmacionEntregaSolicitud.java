package com.perubusSystem.perubus_system.dto;

public record ConfirmacionEntregaSolicitud(String recibidoPor, String dniReceptor) {
    public ConfirmacionEntregaSolicitud(String recibidoPor) {
        this(recibidoPor, null);
    }
}
