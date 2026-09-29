package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ClienteResumen;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClienteControllerTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final ClienteController clienteController = new ClienteController(clienteRepository);

    @Test
    void rechazaDniConFormatoInvalido() {
        var response = clienteController.buscarPorDni("1234");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void devuelveResumenAlEncontrarDniValido() {
        Cliente cliente = new Cliente();
        cliente.setNumDocumento("12345678");
        cliente.setNombres("Ana");
        cliente.setApellidos("Perez");
        cliente.setTelefono("987654321");
        cliente.setCorreo("ana@example.com");
        when(clienteRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(cliente));

        var response = clienteController.buscarPorDni("12345678");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ClienteResumen(
                "Ana", "Perez", "12345678", "987654321", "ana@example.com"));
        verify(clienteRepository).findByNumDocumento("12345678");
    }
}
