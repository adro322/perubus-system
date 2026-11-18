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
    private final Usuario operario = new Usuario();

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
        operario.setIdUsuario(5L);
        operario.setRol("OPERARIO");
        operario.setEstado(true);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(operario));
        when(encomiendaRepository.findByCodigoTracking("PERU-ABCDEFGH")).thenReturn(Optional.of(encomienda));
    }

    @Test
    void operarioActivoPuedeConfirmarLlegadaYGeneraEvento() {
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.confirmarLlegada("PERU-ABCDEFGH", 5L);

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Llegó a agencia de destino");
        verify(encomiendaRepository).save(encomienda);
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void operarioActivoPuedeConfirmarEntregaRegistrandoQuienRecibio() {
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
        operario.setEstado(false);

        assertThatThrownBy(() -> service.confirmarLlegada("PERU-ABCDEFGH", 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void agenteDeVentanillaNoPuedeActualizarEstados() {
        operario.setRol("AGENTE");

        assertThatThrownBy(() -> service.confirmarLlegada("PERU-ABCDEFGH", 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void supervisorPuedeActualizarEstados() {
        operario.setRol("SUPERVISOR");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Encomienda resultado = service.confirmarLlegada("PERU-ABCDEFGH", 5L);

        assertThat(resultado.getEstadoLogistico()).isEqualTo("Llegó a agencia de destino");
    }

    @Test
    void simularPagoEmiteBoletaParaDniYFacturaParaRuc() {
        ComprobanteRepository comprobanteRepo = mock(ComprobanteRepository.class);
        ReflectionTestUtils.setField(service, "comprobanteRepository", comprobanteRepo);
        when(comprobanteRepo.save(any())).thenAnswer(inv -> {
            com.perubusSystem.perubus_system.model.Comprobante c = inv.getArgument(0);
            if (c.getIdComprobante() == null) {
                c.setIdComprobante(15L);
            }
            return c;
        });

        com.perubusSystem.perubus_system.model.Cliente remitente = new com.perubusSystem.perubus_system.model.Cliente();
        remitente.setNumDocumento("12345678");
        encomienda.setRemitente(remitente);
        encomienda.setTarifaBase(20.0);
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_PENDIENTE_PAGO);

        var boleta = service.simularPago("PERU-ABCDEFGH");
        assertThat(boleta.getTipoComprobante()).isEqualTo("BOLETA");
        assertThat(boleta.getNumeroSerie()).isEqualTo("B001-000015");
        assertThat(boleta.getMontoSubtotal()).isEqualByComparingTo("20.00");
        assertThat(boleta.getMontoIgv()).isEqualByComparingTo("3.60");
        assertThat(boleta.getMontoTotal()).isEqualByComparingTo("23.60");

        // Rechaza factura si el cliente solo tiene DNI
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_PENDIENTE_PAGO);
        assertThatThrownBy(() -> service.simularPago("PERU-ABCDEFGH", "FACTURA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RUC de 11 dígitos");

        // Emite factura automáticamente si el documento es RUC de 11 dígitos
        remitente.setNumDocumento("20100070970");
        var factura = service.simularPago("PERU-ABCDEFGH", null);
        assertThat(factura.getTipoComprobante()).isEqualTo("FACTURA");
        assertThat(factura.getNumeroSerie()).isEqualTo("F001-000015");
    }

    @Test
    void confirmarEntregaValidaDniDelReceptor() {
        encomienda.setEstadoLogistico("Disponible para recojo");
        when(encomiendaRepository.save(any(Encomienda.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.confirmarEntrega("PERU-ABCDEFGH", 5L, "Rosa Aybar", "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DNI válido");

        Encomienda entregada = service.confirmarEntrega("PERU-ABCDEFGH", 5L, "Rosa Aybar", "45678912");
        assertThat(entregada.getEstadoLogistico()).isEqualTo("Recogido");
    }
}
