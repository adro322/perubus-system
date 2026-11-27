package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ClienteRespuesta;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.service.CuentaPortalService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClienteControllerTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final CuentaPortalService cuentaPortalService = mock(CuentaPortalService.class);
    private final ClienteController clienteController = new ClienteController();

    ClienteControllerTest() {
        ReflectionTestUtils.setField(clienteController, "clienteRepository", clienteRepository);
        ReflectionTestUtils.setField(clienteController, "cuentaPortalService", cuentaPortalService);
    }

    @Test
    void elAltaFuerzaSinCuentaDelPortalYRechazaDocumentosRepetidos() {
        Cliente nuevo = new Cliente();
        nuevo.setNumDocumento("45218763");
        nuevo.setNombres("Carlos");
        nuevo.setPassword("intentando-colar-una-clave");
        when(clienteRepository.save(org.mockito.ArgumentMatchers.any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        var ok = clienteController.registrarCliente(nuevo);

        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(nuevo.getPassword()).isNull();
        assertThat(((ClienteRespuesta) ok.getBody()).cuentaPortal()).isEqualTo("SIN_CUENTA");

        when(clienteRepository.findByNumDocumento("45218763")).thenReturn(Optional.of(nuevo));
        assertThat(clienteController.registrarCliente(nuevo).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Cliente malo = new Cliente();
        malo.setNumDocumento("123");
        assertThat(clienteController.registrarCliente(malo).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void generarAccesoDevuelveLaClaveTemporalUnaSolaVez() {
        Cliente c = new Cliente();
        c.setIdCliente(4L);
        c.setNumDocumento("45218763");
        c.setNombres("Carlos");
        c.setApellidos("Mendoza");
        when(cuentaPortalService.generarAcceso(4L))
                .thenReturn(new CuentaPortalService.AccesoGenerado(c, "Abc123xyz9", false));

        var r = clienteController.generarAcceso(4L);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> cuerpo = (Map<?, ?>) r.getBody();
        assertThat(cuerpo.get("passwordTemporal")).isEqualTo("Abc123xyz9");
        assertThat(cuerpo.get("accion")).isEqualTo("GENERADO");
        assertThat(cuerpo.get("nombre")).isEqualTo("Carlos Mendoza");
    }

    @Test
    void generarAccesoTraduceLosErroresDelServicio() {
        when(cuentaPortalService.generarAcceso(9L)).thenThrow(new NoSuchElementException("No se encontró el cliente."));
        when(cuentaPortalService.generarAcceso(8L)).thenThrow(new IllegalStateException("La cuenta del portal está bloqueada."));

        assertThat(clienteController.generarAcceso(9L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(clienteController.generarAcceso(8L).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void bloqueoExigeIndicarElValor() {
        assertThat(clienteController.cambiarBloqueo(1L, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void bloqueoDevuelveElClienteConSuNuevoEstado() {
        Cliente c = new Cliente();
        c.setIdCliente(4L);
        c.setPassword("x");
        c.setPortalBloqueado(true);
        when(cuentaPortalService.cambiarBloqueo(4L, true)).thenReturn(c);

        var r = clienteController.cambiarBloqueo(4L, Map.of("bloqueado", true));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((ClienteRespuesta) r.getBody()).cuentaPortal()).isEqualTo("BLOQUEADA");
    }

    @Test
    void rechazaDocumentoConFormatoInvalido() {
        var response = clienteController.buscarPorDni("1234");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void devuelveClienteSinPasswordAlEncontrarDniValido() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setNumDocumento("12345678");
        cliente.setTipoDocumento("DNI");
        cliente.setNombres("Ana");
        cliente.setApellidos("Perez");
        cliente.setTelefono("987654321");
        cliente.setCorreo("ana@example.com");
        cliente.setPassword("secreto");
        when(clienteRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(cliente));

        var response = clienteController.buscarPorDni("12345678");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ClienteRespuesta(
                1L, "12345678", "DNI", "Ana", "Perez", "987654321", "ana@example.com", "ACTIVA"));
        verify(clienteRepository).findByNumDocumento("12345678");
    }
}
