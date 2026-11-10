package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.PortalRegistroSolicitud;
import com.perubusSystem.perubus_system.dto.PortalEncomiendaRespuesta;
import com.perubusSystem.perubus_system.dto.PortalLoginSolicitud;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Manifiesto;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.service.EstimacionEntregaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PortalClienteControllerTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final EncomiendaRepository encomiendaRepository = mock(EncomiendaRepository.class);
    private final EventoTrackingRepository eventoTrackingRepository = mock(EventoTrackingRepository.class);
    private final PortalClienteController controller = new PortalClienteController(
            clienteRepository, encomiendaRepository, eventoTrackingRepository,
            new EstimacionEntregaService(new StandardEnvironment()));

    @Test
    void creaCuentaConContrasenaHasheadaParaClienteExistente() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(12L);
        cliente.setNumDocumento("12345678");
        cliente.setCorreo("ana@example.com");
        when(clienteRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = controller.registrar(new PortalRegistroSolicitud(
                "12345678", "Ana", "Perez", "987654321", "ana@example.com", "Clave-segura-1"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cliente.getPassword()).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("Clave-segura-1", cliente.getPassword())).isTrue();
    }

    @Test
    void rechazaDocumentoInvalidoSinConsultarLaBaseDeDatos() {
        var respuesta = controller.registrar(new PortalRegistroSolicitud(
                "1234", "Ana", "Perez", "987654321", "ana@example.com", "Clave-segura-1"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void primerLoginMigraContrasenaAntiguaEnTextoPlanoAHashBcrypt() {
        Cliente cliente = cliente(12L, "Andree");
        cliente.setNumDocumento("75344786");
        cliente.setPassword("clave-antigua");
        when(clienteRepository.findByNumDocumento("75344786")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        MockHttpServletRequest request = new MockHttpServletRequest();

        var respuesta = controller.iniciarSesion(
                new PortalLoginSolicitud("75344786", "clave-antigua"),
                request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cliente.getPassword()).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("clave-antigua", cliente.getPassword())).isTrue();
        assertThat(request.getSession(false).getAttribute("portalClienteId")).isEqualTo(12L);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void rechazaContrasenaAntiguaIncorrectaSinModificarla() {
        Cliente cliente = cliente(12L, "Andree");
        cliente.setNumDocumento("75344786");
        cliente.setPassword("clave-antigua");
        when(clienteRepository.findByNumDocumento("75344786")).thenReturn(Optional.of(cliente));

        var respuesta = controller.iniciarSesion(
                new PortalLoginSolicitud("75344786", "otra-clave"),
                new MockHttpServletRequest());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(cliente.getPassword()).isEqualTo("clave-antigua");
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void protegeConsultaDeEncomiendasSinSesion() {
        var respuesta = controller.misEncomiendas(new MockHttpSession());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(encomiendaRepository, eventoTrackingRepository);
    }

    @Test
    void clienteDestinatarioSoloConsultaEnviosVinculadosASuCuenta() {
        Cliente destinatario = cliente(2L, "Destinatario");
        Cliente remitente = cliente(1L, "Remitente");
        Encomienda encomienda = new Encomienda();
        encomienda.setIdEncomienda(7L);
        encomienda.setCodigoTracking("PERU-ABCDEFGH");
        encomienda.setEstadoLogistico("En tránsito");
        encomienda.setRuta("Lima-Ica");
        encomienda.setManifiesto(manifiesto(LocalDate.of(2027, 1, 1)));
        encomienda.setDestinatario(destinatario);
        encomienda.setRemitente(remitente);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("portalClienteId", destinatario.getIdCliente());

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(destinatario));
        when(encomiendaRepository
                .findByRemitente_IdClienteOrDestinatario_IdClienteOrderByIdEncomiendaDesc(2L, 2L))
                .thenReturn(List.of(encomienda));
        when(eventoTrackingRepository.findByEncomienda_IdEncomiendaOrderByFechaAsc(7L)).thenReturn(List.of());

        var respuesta = controller.misEncomiendas(session);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var envios = (List<com.perubusSystem.perubus_system.dto.PortalEncomiendaRespuesta>) respuesta.getBody();
        assertThat(envios).hasSize(1);
        assertThat(envios.get(0).rolCliente()).isEqualTo("Destinatario");
        assertThat(envios.get(0).contraparte()).isEqualTo("Remitente Prueba");
        assertThat(envios.get(0).progreso()).isEqualTo(50);
        assertThat(envios.get(0).fechaEstimadaLlegada()).isEqualTo(LocalDate.of(2027, 1, 3));
        assertThat(envios.get(0).estimacionVencida()).isFalse();
        verify(encomiendaRepository)
                .findByRemitente_IdClienteOrDestinatario_IdClienteOrderByIdEncomiendaDesc(2L, 2L);
        verify(encomiendaRepository, never()).findAll();
    }

    @Test
    void fechaEstimadaVencidaNoMarcaAutomaticamenteLaLlegada() {
        LocalDate fechaViaje = LocalDate.now().minusDays(19);
        Cliente remitente = cliente(1L, "Remitente");
        Cliente destinatario = cliente(2L, "Destinatario");
        Encomienda encomienda = new Encomienda();
        encomienda.setCodigoTracking("PERU-ABCDEFGH");
        encomienda.setEstadoLogistico("En tránsito");
        encomienda.setRuta("Lima-Ica");
        encomienda.setRemitente(remitente);
        encomienda.setDestinatario(destinatario);
        encomienda.setManifiesto(manifiesto(fechaViaje));

        var respuesta = PortalEncomiendaRespuesta.desde(
                encomienda, 1L, fechaViaje.plusDays(2), List.of());

        assertThat(respuesta.fechaEstimadaLlegada()).isEqualTo(fechaViaje.plusDays(2));
        assertThat(respuesta.estimacionVencida()).isTrue();
        assertThat(respuesta.progreso()).isEqualTo(50);
        assertThat(respuesta.estadoActual()).isEqualTo("En tránsito");
    }

    private Manifiesto manifiesto(LocalDate fechaViaje) {
        Manifiesto manifiesto = new Manifiesto();
        manifiesto.setFechaViaje(fechaViaje);
        return manifiesto;
    }

    private Cliente cliente(Long id, String nombres) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(id);
        cliente.setNombres(nombres);
        cliente.setApellidos("Prueba");
        return cliente;
    }
}
