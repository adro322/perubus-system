package com.perubusSystem.perubus_system.dto;

// tipo: "DNI" o "RUC". Para RUC, nombres lleva la razón social y apellidos va vacío.
// origen: "LOCAL" (ya registrado en nuestra BD) o "API" (consultado a Decolecta)
public record DocumentoRespuesta(
        String tipo,
        String numero,
        String nombres,
        String apellidos,
        String origen) {
}
