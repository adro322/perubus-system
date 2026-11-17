package com.perubusSystem.perubus_system.seguridad;

import com.perubusSystem.perubus_system.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccesoPorRolInterceptorTest {

    // Controlador de prueba con las tres variantes de permiso
    static class Prueba {
        @RequiereRol(Rol.AGENTE)
        public void paraAgente() {
        }

        @RequiereRol(Rol.OPERARIO)
        public void paraOperario() {
        }

        @RequiereRol({})
        public void soloSuperusuarios() {
        }

        public void cualquierPersonal() {
        }
    }

    private final SesionPersonal sesionPersonal = mock(SesionPersonal.class);
    private final AccesoPorRolInterceptor interceptor = new AccesoPorRolInterceptor();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prueba");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(interceptor, "sesionPersonal", sesionPersonal);
    }

    private void conRol(String rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(1L);
        u.setRol(rol);
        u.setEstado(true);
        when(sesionPersonal.actual(any())).thenReturn(Optional.of(u));
    }

    private boolean acceso(String metodo) throws Exception {
        HandlerMethod handler = new HandlerMethod(new Prueba(), Prueba.class.getMethod(metodo));
        return interceptor.preHandle(request, response, handler);
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        when(sesionPersonal.actual(any())).thenReturn(Optional.empty());

        assertThat(acceso("paraAgente")).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void adminYSupervisorTienenAccesoTotal() throws Exception {
        for (String rol : new String[]{"ADMIN", "SUPERVISOR", "admin"}) {
            conRol(rol);
            for (String metodo : new String[]{"paraAgente", "paraOperario", "soloSuperusuarios", "cualquierPersonal"}) {
                assertThat(acceso(metodo)).as(rol + " -> " + metodo).isTrue();
            }
        }
    }

    @Test
    void agenteSoloUsaLoDeVentanilla() throws Exception {
        conRol("AGENTE");

        assertThat(acceso("paraAgente")).isTrue();
        assertThat(acceso("cualquierPersonal")).isTrue();
        assertThat(acceso("paraOperario")).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void operarioSoloUsaLoOperativo() throws Exception {
        conRol("OPERARIO");

        assertThat(acceso("paraOperario")).isTrue();
        assertThat(acceso("paraAgente")).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void soloSuperusuariosBloqueaAAgenteYOperario() throws Exception {
        conRol("AGENTE");
        assertThat(acceso("soloSuperusuarios")).isFalse();

        conRol("OPERARIO");
        assertThat(acceso("soloSuperusuarios")).isFalse();
    }

    @Test
    void rolDesconocidoNoTieneAcceso() throws Exception {
        conRol("CONDUCTOR");

        assertThat(acceso("cualquierPersonal")).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void laPeticionPreflightCorsNoSeBloquea() throws Exception {
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/prueba");
        HandlerMethod handler = new HandlerMethod(new Prueba(), Prueba.class.getMethod("soloSuperusuarios"));

        assertThat(interceptor.preHandle(preflight, response, handler)).isTrue();
    }
}
