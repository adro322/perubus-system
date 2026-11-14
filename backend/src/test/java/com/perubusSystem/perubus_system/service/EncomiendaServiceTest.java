package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.ComprobanteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncomiendaServiceTest {

    private final EncomiendaRepository encomiendaRepository = mock(EncomiendaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EventoTrackingRepository eventoTrackingRepository = mock(EventoTrackingRepository.class);
    private final EncomiendaService service = new EncomiendaService();
    private final Encomienda encomienda = new Encomienda();
    private final Usuario agente = new Usuario();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "encomiendaRepository", encomiendaRepository);
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(service, "eventoTrackingRepository", eventoTrackingRepository);
        ReflectionTestUtils.setField(service, "clienteRepository", mock(ClienteRepository.class));
        ReflectionTestUtils.setField(service, "comprobanteRepository", mock(ComprobanteRepository.class));

        encomienda.setCodigoTracking("PERU-ABCDEFGH");
        encomienda.setEstadoLogistico("En tránsito");
        encomienda.setRuta("Lima-Ica");
        agente.setIdUsuario(5L);
        agente.setRol("AGENTE");
        agente.setEstado(true);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(agente));
        when(encomiendaRepository.findByCodigoTracking("PERU-ABCDEFGH")).thenReturn(Optional.of(encomienda));
    }

    @Test
    void agenteActivoPuedeConfirmarLlegadaYGeneraEvento() {
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.confirmarLlegada("PERU-ABCDEFGH", 5L);

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Llegó a agencia de destino");
        verify(encomiendaRepository).save(encomienda);
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void agenteActivoPuedeConfirmarEntregaRegistrandoQuienRecibio() {
        encomienda.setEstadoLogistico("Llegó a agencia de destino");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.confirmarEntrega("PERU-ABCDEFGH", 5L, "Rosa Aybar");

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Recogido");
        verify(encomiendaRepository).save(encomienda);
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void encomiendaEnAgenciaPuedeQuedarDisponibleParaRecojo() {
        encomienda.setEstadoLogistico("Llegó a agencia de destino");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.marcarDisponibleParaRecojo("PERU-ABCDEFGH", 5L);

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Disponible para recojo");
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void encomiendaEnAgenciaPuedeIniciarRepartoADomicilio() {
        encomienda.setEstadoLogistico("Llegó a agencia de destino");
        encomienda.setDireccionDestinatario("Av. Prueba 123");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.iniciarReparto("PERU-ABCDEFGH", 5L);

        assertThat(resultado.getEstadoLogistico()).isEqualTo("En reparto");
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void noPermiteIniciarRepartoSinDireccionDelDestinatario() {
        encomienda.setEstadoLogistico("Llegó a agencia de destino");

        assertThatThrownBy(() -> service.iniciarReparto("PERU-ABCDEFGH", 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dirección del destinatario");
    }

    @Test
    void repartoConfirmadoFinalizaComoEntregado() {
        encomienda.setEstadoLogistico("En reparto");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.confirmarEntrega("PERU-ABCDEFGH", 5L, "Rosa Aybar");

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Entregado");
    }

    @Test
    void recojoConfirmadoFinalizaComoRecogido() {
        encomienda.setEstadoLogistico("Disponible para recojo");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.confirmarEntrega("PERU-ABCDEFGH", 5L, "Rosa Aybar");

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Recogido");
    }

    @Test
    void noPermiteEntregaAntesDeLlegarALaAgencia() {
        assertThatThrownBy(() -> service.confirmarEntrega("PERU-ABCDEFGH", 5L, "Rosa Aybar"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agencia de destino");
    }

    @Test
    void rechazaConfirmarLlegadaSiLaEncomiendaNoEstaEnTransito() {
        encomienda.setEstadoLogistico("En origen");

        assertThatThrownBy(() -> service.confirmarLlegada("PERU-ABCDEFGH", 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("en tránsito");
    }

    @Test
    void rechazaUsuarioInactivoAntesDeCambiarElEstado() {
        agente.setEstado(false);

        assertThatThrownBy(() -> service.confirmarLlegada("PERU-ABCDEFGH", 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes permiso");
    }
}
