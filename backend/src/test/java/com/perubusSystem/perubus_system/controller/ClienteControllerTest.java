package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ClienteRespuesta;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClienteControllerTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final ClienteController clienteController = new ClienteController();

    ClienteControllerTest() {
        ReflectionTestUtils.setField(clienteController, "clienteRepository", clienteRepository);
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
                1L, "12345678", "DNI", "Ana", "Perez", "987654321", "ana@example.com"));
        verify(clienteRepository).findByNumDocumento("12345678");
    }
}
