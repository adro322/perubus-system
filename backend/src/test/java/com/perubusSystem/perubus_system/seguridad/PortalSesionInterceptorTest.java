package com.perubusSystem.perubus_system.seguridad;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortalSesionInterceptorTest {

    private final ClienteRepository repo = mock(ClienteRepository.class);
    private final PortalSesionInterceptor interceptor = new PortalSesionInterceptor();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/portal/mis-encomiendas");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockHttpSession sesion = new MockHttpSession();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(interceptor, "clienteRepository", repo);
        request.setSession(sesion);
    }

    @Test
    void sinSesionDeClienteDejaPasar() throws Exception {
        MockHttpServletRequest sinSesion = new MockHttpServletRequest("GET", "/api/portal/sesion");

        assertThat(interceptor.preHandle(sinSesion, response, new Object())).isTrue();
    }

    @Test
    void clienteActivoSigueConSuSesion() throws Exception {
        sesion.setAttribute("portalClienteId", 5L);
        Cliente activo = new Cliente();
        when(repo.findById(5L)).thenReturn(Optional.of(activo));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(sesion.isInvalid()).isFalse();
    }

    @Test
    void clienteBloqueadoPierdeLaSesion() throws Exception {
        sesion.setAttribute("portalClienteId", 5L);
        Cliente bloqueado = new Cliente();
        bloqueado.setPortalBloqueado(true);
        when(repo.findById(5L)).thenReturn(Optional.of(bloqueado));

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(sesion.isInvalid()).isTrue();
    }

    @Test
    void clienteEliminadoPierdeLaSesion() throws Exception {
        sesion.setAttribute("portalClienteId", 5L);
        when(repo.findById(5L)).thenReturn(Optional.empty());

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(sesion.isInvalid()).isTrue();
    }

    @Test
    void laPeticionPreflightNoSeBloquea() throws Exception {
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/portal/mis-encomiendas");

        assertThat(interceptor.preHandle(preflight, response, new Object())).isTrue();
    }
}
