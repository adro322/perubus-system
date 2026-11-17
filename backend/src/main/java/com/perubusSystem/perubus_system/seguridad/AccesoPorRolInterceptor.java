package com.perubusSystem.perubus_system.seguridad;

import com.perubusSystem.perubus_system.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Protege /api/**: exige sesión de personal activo y valida el rol según @RequiereRol.
@Component
public class AccesoPorRolInterceptor implements HandlerInterceptor {

    @Autowired
    private SesionPersonal sesionPersonal;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod metodo) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Optional<Usuario> personal = sesionPersonal.actual(request);
        if (personal.isEmpty()) {
            return rechazar(response, 401, "Inicia sesión como personal para continuar.");
        }

        Optional<Rol> rol = Rol.desde(personal.get().getRol());
        if (rol.isEmpty()) {
            return rechazar(response, 403, "Tu usuario no tiene un rol válido asignado.");
        }

        RequiereRol requisito = metodo.getMethodAnnotation(RequiereRol.class);
        if (requisito == null) {
            requisito = metodo.getBeanType().getAnnotation(RequiereRol.class);
        }
        if (requisito != null && !rol.get().autoriza(requisito.value())) {
            return rechazar(response, 403, "Tu rol (" + rol.get().name() + ") no tiene permiso para esta acción.");
        }

        request.setAttribute(SesionPersonal.ATRIBUTO_USUARIO, personal.get());
        return true;
    }

    private boolean rechazar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"mensaje\":\"" + mensaje.replace("\"", "'") + "\"}");
        return false;
    }
}
