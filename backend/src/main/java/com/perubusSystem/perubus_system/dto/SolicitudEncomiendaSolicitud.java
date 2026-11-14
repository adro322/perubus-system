package com.perubusSystem.perubus_system.dto;

public record SolicitudEncomiendaSolicitud(
        String tipo,
        String direccionDestinatario,
        String descripcionBulto,
        Double pesoBulto) {

    public SolicitudEncomiendaSolicitud(String tipo, String direccionDestinatario) {
        this(tipo, direccionDestinatario, null, null);
    }
}
