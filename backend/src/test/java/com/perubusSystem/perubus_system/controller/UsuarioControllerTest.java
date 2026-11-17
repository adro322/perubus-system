package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.UsuarioRespuesta;
import com.perubusSystem.perubus_system.dto.UsuarioSolicitud;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import com.perubusSystem.perubus_system.seguridad.SesionPersonal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioControllerTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final SesionPersonal sesion = mock(SesionPersonal.class);
    private final UsuarioController controller = new UsuarioController();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(controller, "usuarioRepository", repo);
        ReflectionTestUtils.setField(controller, "sesionPersonal", sesion);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(Long id, String rol, boolean activo) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setUsername("user" + id);
        u.setRol(rol);
        u.setEstado(activo);
        return u;
    }

    private void sesionDe(Usuario u) {
        when(sesion.actual(any())).thenReturn(Optional.of(u));
    }

    @Test
    void tras5FallosElLoginDelPersonalQuedaLimitado() {
        when(repo.findByUsernameAndPassword(any(), any())).thenReturn(Optional.empty());
        Usuario credenciales = new Usuario();
        credenciales.setUsername("Admin.Perubus");
        credenciales.setPassword("incorrecta");

        for (int i = 0; i < 5; i++) {
            assertThat(controller.autenticarUsuario(credenciales, request).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        assertThat(controller.autenticarUsuario(credenciales, request).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void sinSesionNoSePuedeRegistrarPersonal() {
        when(repo.count()).thenReturn(3L);
        when(sesion.actual(any())).thenReturn(Optional.empty());

        var r = controller.registrarUsuario(new UsuarioSolicitud("nuevo", "secreto1", "ADMIN", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(repo, never()).save(any());
    }

    @Test
    void unAgenteNoPuedeRegistrarPersonal() {
        when(repo.count()).thenReturn(3L);
        sesionDe(usuario(2L, "AGENTE", true));

        var r = controller.registrarUsuario(new UsuarioSolicitud("nuevo", "secreto1", "ADMIN", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).save(any());
    }

    @Test
    void elAdminRegistraPersonalYNoDevuelveLaContrasena() {
        when(repo.count()).thenReturn(3L);
        sesionDe(usuario(1L, "ADMIN", true));

        var r = controller.registrarUsuario(new UsuarioSolicitud("nuevo", "secreto1", "operario", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        var cuerpo = (UsuarioRespuesta) r.getBody();
        assertThat(cuerpo.rol()).isEqualTo("OPERARIO");
        assertThat(cuerpo.estado()).isTrue();
    }

    @Test
    void rechazaRolesQueNoExisten() {
        when(repo.count()).thenReturn(3L);
        sesionDe(usuario(1L, "ADMIN", true));

        var r = controller.registrarUsuario(new UsuarioSolicitud("nuevo", "secreto1", "CONDUCTOR", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rechazaUsuarioRepetido() {
        when(repo.count()).thenReturn(3L);
        sesionDe(usuario(1L, "ADMIN", true));
        when(repo.existsByUsernameIgnoreCase("nuevo")).thenReturn(true);

        var r = controller.registrarUsuario(new UsuarioSolicitud("nuevo", "secreto1", "AGENTE", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elPrimerUsuarioDelSistemaSeCreaSinSesionYSiempreComoAdmin() {
        when(repo.count()).thenReturn(0L);

        var r = controller.registrarUsuario(new UsuarioSolicitud("root", "secreto1", "OPERARIO", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((UsuarioRespuesta) r.getBody()).rol()).isEqualTo("ADMIN");
    }

    @Test
    void noPermiteQuitarseElRolOElAccesoASiMismo() {
        Usuario admin = usuario(1L, "ADMIN", true);
        sesionDe(admin);
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        when(repo.findAll()).thenReturn(List.of(admin, usuario(2L, "SUPERVISOR", true)));

        var r = controller.actualizar(1L, new UsuarioSolicitud(null, null, "AGENTE", null), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(admin.getRol()).isEqualTo("ADMIN");
    }

    @Test
    void noPermiteEliminarAlUltimoAdministrador() {
        Usuario unico = usuario(1L, "ADMIN", true);
        sesionDe(usuario(9L, "SUPERVISOR", false)); // otra sesión cualquiera
        when(repo.findById(1L)).thenReturn(Optional.of(unico));
        when(repo.findAll()).thenReturn(List.of(unico, usuario(3L, "AGENTE", true)));

        var r = controller.eliminar(1L, request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(repo, never()).delete(any());
    }

    @Test
    void elAdminPuedeCambiarElRolDeUnAgente() {
        sesionDe(usuario(1L, "ADMIN", true));
        Usuario agente = usuario(5L, "AGENTE", true);
        when(repo.findById(5L)).thenReturn(Optional.of(agente));

        var r = controller.actualizar(5L, new UsuarioSolicitud(null, null, "OPERARIO", false), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(agente.getRol()).isEqualTo("OPERARIO");
        assertThat(agente.getEstado()).isFalse();
    }
}
