package com.perubusSystem.perubus_system.seguridad;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Si el personal bloquea (o elimina) la cuenta de un cliente mientras tiene la sesión abierta,
// la sesión se cierra en su siguiente petición al portal.
@Component
public class PortalSesionInterceptor implements HandlerInterceptor {

    static final String SESION_CLIENTE_ID = "portalClienteId";

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute(SESION_CLIENTE_ID) instanceof Long id)) {
            return true; // sin sesión de cliente: cada endpoint responde por su cuenta
        }
        Optional<Cliente> cliente = clienteRepository.findById(id);
        if (cliente.isPresent() && !cliente.get().cuentaBloqueada()) {
            return true;
        }
        sesion.invalidate();
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"mensaje\":\"Tu sesión terminó. Inicia sesión nuevamente.\"}");
        return false;
    }
}
