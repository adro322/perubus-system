package com.perubusSystem.perubus_system.util;

import java.util.Locale;

public final class NombrePersona {

    private static final Locale LOCALE_ESPANOL = Locale.forLanguageTag("es");

    private NombrePersona() {
    }

    public static String formatoTitulo(String valor) {
        if (valor == null || valor.isBlank())
            return valor;

        String minusculas = valor.trim().toLowerCase(LOCALE_ESPANOL);
        StringBuilder resultado = new StringBuilder(minusculas.length());
        boolean inicioPalabra = true;

        for (int indice = 0; indice < minusculas.length();) {
            int caracter = minusculas.codePointAt(indice);
            if (Character.isLetter(caracter)) {
                resultado.appendCodePoint(inicioPalabra ? Character.toTitleCase(caracter) : caracter);
                inicioPalabra = false;
            } else {
                resultado.appendCodePoint(caracter);
                inicioPalabra = !Character.isDigit(caracter);
            }
            indice += Character.charCount(caracter);
        }

        return resultado.toString();
    }
}