package com.perubusSystem.perubus_system.seguridad;

import java.util.Arrays;
import java.util.Optional;

// Roles del personal. ADMIN y SUPERVISOR tienen acceso total; los demás solo a lo que se les asigna.
public enum Rol {
    ADMIN,
    SUPERVISOR,
    AGENTE,
    OPERARIO;

    public static Optional<Rol> desde(String texto) {
        if (texto == null) return Optional.empty();
        String normalizado = texto.trim();
        return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(normalizado)).findFirst();
    }

    public boolean esSuperusuario() {
        return this == ADMIN || this == SUPERVISOR;
    }

    // Verdadero si el rol es superusuario o está entre los permitidos
    public boolean autoriza(Rol... permitidos) {
        return esSuperusuario() || Arrays.asList(permitidos).contains(this);
    }

    // Igual que autoriza, pero partiendo del texto guardado en la base de datos
    public static boolean autoriza(String rolTexto, Rol... permitidos) {
        return desde(rolTexto).map(r -> r.autoriza(permitidos)).orElse(false);
    }

    public static boolean esSuperusuario(String rolTexto) {
        return desde(rolTexto).map(Rol::esSuperusuario).orElse(false);
    }
}
