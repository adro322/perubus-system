package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CuentaPortalServiceTest {

    private final ClienteRepository repo = mock(ClienteRepository.class);
    private final CuentaPortalService service = new CuentaPortalService();
    private final Cliente cliente = new Cliente();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "clienteRepository", repo);
        cliente.setIdCliente(7L);
        cliente.setNumDocumento("45218763");
        cliente.setNombres("Carlos");
        when(repo.findById(7L)).thenReturn(Optional.of(cliente));
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generaUnaCuentaNuevaConClaveTemporalCifrada() {
        var acceso = service.generarAcceso(7L);

        assertThat(acceso.restablecido()).isFalse();
        assertThat(acceso.passwordTemporal()).hasSize(10).matches("[A-HJ-NP-Za-km-z2-9]+");
        assertThat(cliente.getPassword()).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches(acceso.passwordTemporal(), cliente.getPassword())).isTrue();
        assertThat(cliente.debeCambiarClave()).isTrue();
        verify(repo).save(cliente);
    }

    @Test
    void restablecerCambiaLaClaveYExigeCambiarlaOtraVez() {
        String anterior = new BCryptPasswordEncoder().encode("MiClaveActual1");
        cliente.setPassword(anterior);
        cliente.setDebeCambiarPassword(false);

        var acceso = service.generarAcceso(7L);

        assertThat(acceso.restablecido()).isTrue();
        assertThat(cliente.getPassword()).isNotEqualTo(anterior);
        assertThat(new BCryptPasswordEncoder().matches("MiClaveActual1", cliente.getPassword())).isFalse();
        assertThat(cliente.debeCambiarClave()).isTrue();
    }

    @Test
    void cadaClaveTemporalEsDistinta() {
        assertThat(service.generarClave()).isNotEqualTo(service.generarClave());
    }

    @Test
    void unaCuentaBloqueadaNoSeRestablece() {
        cliente.setPassword("x");
        cliente.setPortalBloqueado(true);

        assertThatThrownBy(() -> service.generarAcceso(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloqueada");
    }

    @Test
    void clienteInexistente() {
        assertThatThrownBy(() -> service.generarAcceso(99L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void soloSePuedeBloquearUnaCuentaQueExiste() {
        assertThatThrownBy(() -> service.cambiarBloqueo(7L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aún no tiene");
    }

    @Test
    void bloquearYDesbloquear() {
        cliente.setPassword("x");

        service.cambiarBloqueo(7L, true);
        assertThat(cliente.cuentaBloqueada()).isTrue();

        service.cambiarBloqueo(7L, false);
        assertThat(cliente.cuentaBloqueada()).isFalse();
    }
}
