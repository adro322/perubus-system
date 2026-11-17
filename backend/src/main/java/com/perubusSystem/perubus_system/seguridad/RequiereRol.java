package com.perubusSystem.perubus_system.seguridad;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Roles que pueden usar el endpoint además de ADMIN y SUPERVISOR (que siempre pasan).
// Sin roles (@RequiereRol({})) = solo ADMIN y SUPERVISOR.
// Sin la anotación = cualquier personal activo con sesión iniciada.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiereRol {
    Rol[] value() default {};
}
