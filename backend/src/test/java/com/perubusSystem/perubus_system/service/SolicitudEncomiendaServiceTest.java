package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.dto.DecisionSolicitudEncomienda;
import com.perubusSystem.perubus_system.dto.SolicitudEncomiendaSolicitud;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.BultoEncomienda;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.SolicitudEncomienda;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.BultoEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.repository.SolicitudEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitudEncomiendaServiceTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final BultoEncomiendaRepository bultoRepository = mock(BultoEncomiendaRepository.class);
    private final EncomiendaRepository encomiendaRepository = mock(EncomiendaRepository.class);
    private final EventoTrackingRepository eventoTrackingRepository = mock(EventoTrackingRepository.class);
    private final SolicitudEncomiendaRepository solicitudRepository = mock(SolicitudEncomiendaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EncomiendaService encomiendaService = mock(EncomiendaService.class);
    private final SolicitudEncomiendaService service = new SolicitudEncomiendaService(
            clienteRepository, bultoRepository, encomiendaRepository, eventoTrackingRepository, solicitudRepository,
            usuarioRepository, encomiendaService);
    private final Cliente remitente = new Cliente();
    private final Cliente destinatario = new Cliente();
    private final Encomienda encomienda = new Encomienda();
    private final Usuario agente = new Usuario();

    @BeforeEach
    void preparar() {
        remitente.setIdCliente(1L);
        remitente.setNombres("Ana");
        remitente.setApellidos("Prueba");
        destinatario.setIdCliente(2L);
        encomienda.setIdEncomienda(10L);
        encomienda.setCodigoTracking("PERU-ABCDEFGH");
        encomienda.setRemitente(remitente);
        encomienda.setDestinatario(destinatario);
        encomienda.setDireccionDestinatario("Calle anterior 123");
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_PENDIENTE_PAGO);
        agente.setIdUsuario(5L);
        agente.setRol("AGENTE");
        agente.setEstado(true);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(remitente));
        when(encomiendaRepository.findByCodigoTracking("PERU-ABCDEFGH")).thenReturn(Optional.of(encomienda));
        when(solicitudRepository.save(any(SolicitudEncomienda.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(agente));
        when(encomiendaService.calcularTarifaRuta("Lima-Ica", 3.0)).thenReturn(java.math.BigDecimal.valueOf(16.50));
        when(encomiendaService.calcularTarifaRuta("Lima-Ica", 6.0)).thenReturn(java.math.BigDecimal.valueOf(33.00));
    }

    @Test
    void aplicaCambioDeDireccionAntesDelDespachoYRegistraEvento() {
        var resultado = service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CAMBIO_DIRECCION", "Av. Nueva 456"));

        assertThat(encomienda.getDireccionDestinatario()).isEqualTo("Av. Nueva 456");
        assertThat(resultado.estado()).isEqualTo(SolicitudEncomiendaService.APLICADA);
        verify(encomiendaRepository).save(encomienda);
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void bultoAntesDelPagoRequiereAprobacionYRecalculaLaTarifa() {
        encomienda.setPeso(2.0);
        encomienda.setPesoOriginal(2.0);
        encomienda.setRuta("Lima-Ica");
        encomienda.setTarifaBase(15.0);

        var creada = service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("AGREGAR_BULTO", null, "Caja de libros", 1.0));

        assertThat(creada.estado()).isEqualTo(SolicitudEncomiendaService.PENDIENTE);
        assertThat(encomienda.getPeso()).isEqualTo(2.0);
        assertThat(encomienda.getTarifaBase()).isEqualTo(15.0);

        SolicitudEncomienda solicitud = solicitud("AGREGAR_BULTO", "PENDIENTE", null);
        solicitud.setDescripcionBulto("Caja de libros");
        solicitud.setPesoBulto(1.0);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));
        var aprobada = service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, ""));

        assertThat(aprobada.estado()).isEqualTo(SolicitudEncomiendaService.APROBADA);
        assertThat(encomienda.getPeso()).isEqualTo(3.0);
        assertThat(encomienda.getPesoOriginal()).isEqualTo(2.0);
        assertThat(encomienda.getTarifaBase()).isEqualTo(16.50);
        verify(bultoRepository).save(any(BultoEncomienda.class));
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void personalPuedeConsultarBultosAgregadosAutomaticamenteComoRegistroInformativo() {
        SolicitudEncomienda solicitudAplicada = solicitud("AGREGAR_BULTO", "APLICADA", null);
        solicitudAplicada.setDescripcionBulto("Caja con stickers");
        solicitudAplicada.setPesoBulto(50.0);
        solicitudAplicada.setFechaSolicitud(LocalDateTime.now());
        when(solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(SolicitudEncomiendaService.PENDIENTE))
                .thenReturn(List.of());
        when(solicitudRepository.findByTipoOrderByFechaSolicitudDesc("AGREGAR_BULTO"))
                .thenReturn(List.of(solicitudAplicada));

        var visibles = service.listarPendientes(5L);

        assertThat(visibles).hasSize(1);
        assertThat(visibles.get(0).tipo()).isEqualTo("AGREGAR_BULTO");
        assertThat(visibles.get(0).estado()).isEqualTo(SolicitudEncomiendaService.APLICADA);
        assertThat(visibles.get(0).descripcionBulto()).isEqualTo("Caja con stickers");
        assertThat(visibles.get(0).pesoBulto()).isEqualTo(50.0);
    }

    @Test
    void bultoEnEnvioPagadoRequiereAprobacionYDespuesPagoAdicional() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_ORIGEN);
        encomienda.setPeso(5.0);
        encomienda.setPesoOriginal(5.0);
        encomienda.setRuta("Lima-Ica");
        encomienda.setTarifaBase(30.0);

        var solicitudCreada = service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("AGREGAR_BULTO", null, "Bolsa de ropa", 1.0));
        assertThat(solicitudCreada.estado()).isEqualTo(SolicitudEncomiendaService.PENDIENTE);
        assertThat(encomienda.getPeso()).isEqualTo(5.0);

        SolicitudEncomienda solicitud = solicitud("AGREGAR_BULTO", "PENDIENTE", null);
        solicitud.setDescripcionBulto("Bolsa de ropa");
        solicitud.setPesoBulto(1.0);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        var aprobada = service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, ""));
        assertThat(aprobada.estado()).isEqualTo(SolicitudEncomiendaService.PENDIENTE_PAGO);
        assertThat(aprobada.montoAdicional()).isEqualByComparingTo("3.00");
        assertThat(encomienda.getEstadoLogistico()).isEqualTo("Pendiente de pago adicional");
        assertThat(encomienda.getPeso()).isEqualTo(6.0);

        var pagada = service.pagarImporteAdicional(1L, 20L);
        assertThat(pagada.estado()).isEqualTo(SolicitudEncomiendaService.APROBADA);
        assertThat(pagada.fechaPagoAdicional()).isNotNull();
        assertThat(encomienda.getEstadoLogistico()).isEqualTo(EncomiendaService.ESTADO_EN_ORIGEN);
    }

    @Test
    void rechazaPagarCargoAdicionalDeOtraPersona() {
        SolicitudEncomienda solicitud = solicitud("AGREGAR_BULTO", "PENDIENTE_PAGO", null);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.pagarImporteAdicional(2L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo el remitente");
    }

    @Test
    void rechazaBultoConPesoInvalido() {
        assertThatThrownBy(() -> service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("AGREGAR_BULTO", null, "Caja", 0.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("peso del bulto");
    }

    @Test
    void cancelacionAntesDelPagoQuedaPendienteDeAprobacionYNoOcultaElEnvio() {
        var resultado = service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CANCELACION", null));

        assertThat(encomienda.getEstadoLogistico()).isEqualTo(EncomiendaService.ESTADO_PENDIENTE_PAGO);
        assertThat(resultado.estado()).isEqualTo(SolicitudEncomiendaService.PENDIENTE);
        assertThat(resultado.respuesta()).contains("seguirá registrada");
        verify(encomiendaRepository, never()).save(encomienda);
        verify(eventoTrackingRepository, never()).save(any());
    }

    @Test
    void cancelacionDespuesDelPagoYEnOrigenQuedaPendienteDeAprobacion() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_ORIGEN);

        var resultado = service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CANCELACION", null));

        assertThat(resultado.estado()).isEqualTo(SolicitudEncomiendaService.PENDIENTE);
        assertThat(encomienda.getEstadoLogistico()).isEqualTo(EncomiendaService.ESTADO_EN_ORIGEN);
        assertThat(resultado.respuesta()).contains("reembolso");
        verify(encomiendaRepository, never()).save(encomienda);
    }

    @Test
    void cambioDeDireccionDespuesDelPagoYEnOrigenQuedaPendienteDeAprobacion() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_ORIGEN);

        var resultado = service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CAMBIO_DIRECCION", "Av. Nueva 456"));

        assertThat(resultado.estado()).isEqualTo(SolicitudEncomiendaService.PENDIENTE);
        assertThat(encomienda.getDireccionDestinatario()).isEqualTo("Calle anterior 123");
        verify(encomiendaRepository, never()).save(encomienda);
    }

    @Test
    void rechazaCambioDeDireccionDespuesDelDespacho() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_TRANSITO);

        assertThatThrownBy(() -> service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CAMBIO_DIRECCION", "Av. Nueva 456")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salga de origen");
    }

    @Test
    void rechazaCancelacionDespuesDelDespacho() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_TRANSITO);

        assertThatThrownBy(() -> service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CANCELACION", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salga de origen");
    }

    @Test
    void rechazaCambioDeDireccionIgualALaActual() {
        assertThatThrownBy(() -> service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CAMBIO_DIRECCION", " Calle anterior 123 ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferente");
    }

    @Test
    void soloElRemitentePuedeSolicitarCambios() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(destinatario));

        assertThatThrownBy(() -> service.crear(2L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CANCELACION", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo el remitente");
    }

    @Test
    void rechazaDireccionVacia() {
        assertThatThrownBy(() -> service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CAMBIO_DIRECCION", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoria");
    }

    @Test
    void impideSegundaSolicitudMientrasHayUnaPendiente() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_TRANSITO);
        when(solicitudRepository.existsByEncomienda_IdEncomiendaAndEstadoIn(
                10L, java.util.List.of(
                        SolicitudEncomiendaService.PENDIENTE,
                        SolicitudEncomiendaService.PENDIENTE_PAGO))).thenReturn(true);

        assertThatThrownBy(() -> service.crear(1L, "PERU-ABCDEFGH",
                new SolicitudEncomiendaSolicitud("CANCELACION", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una solicitud pendiente");
    }

    @Test
    void personalPuedeAprobarCambioDeDireccion() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_ORIGEN);
        SolicitudEncomienda solicitud = solicitud("CAMBIO_DIRECCION", "PENDIENTE", "Av. Nueva 456");
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        var resultado = service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, "Verificado"));

        assertThat(encomienda.getDireccionDestinatario()).isEqualTo("Av. Nueva 456");
        assertThat(resultado.estado()).isEqualTo(SolicitudEncomiendaService.APROBADA);
        verify(eventoTrackingRepository).save(any());
    }

    @Test
    void personalPuedeAprobarCancelacionYConservaElManifiesto() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_ORIGEN);
        SolicitudEncomienda solicitud = solicitud("CANCELACION", "PENDIENTE", null);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        var resultado = service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, ""));

        assertThat(encomienda.getEstadoLogistico()).isEqualTo("Cancelado");
        assertThat(resultado.estado()).isEqualTo(SolicitudEncomiendaService.APROBADA);
        verify(encomiendaRepository).save(encomienda);
    }

    @Test
    void personalNoPuedeAprobarCancelacionUnaVezDespachada() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_TRANSITO);
        SolicitudEncomienda solicitud = solicitud("CANCELACION", "PENDIENTE", null);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salió de origen");
        verify(encomiendaRepository, never()).save(encomienda);
    }

    @Test
    void personalNoPuedeAprobarCambioDeDireccionUnaVezDespachada() {
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_TRANSITO);
        SolicitudEncomienda solicitud = solicitud("CAMBIO_DIRECCION", "PENDIENTE", "Av. Nueva 456");
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("salió de origen");
    }

    @Test
    void rechazoRequiereUnMotivo() {
        SolicitudEncomienda solicitud = solicitud("CANCELACION", "PENDIENTE", null);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.resolver(20L, 5L, new DecisionSolicitudEncomienda(false, " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void noPermiteAprobarSolicitudAtendida() {
        SolicitudEncomienda solicitud = solicitud("CANCELACION", "RECHAZADA", null);
        when(solicitudRepository.findById(20L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.resolver(20L, 5L, new DecisionSolicitudEncomienda(true, "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue atendida");
    }

    private SolicitudEncomienda solicitud(String tipo, String estado, String direccion) {
        SolicitudEncomienda solicitud = new SolicitudEncomienda();
        solicitud.setIdSolicitud(20L);
        solicitud.setEncomienda(encomienda);
        solicitud.setCliente(remitente);
        solicitud.setTipo(tipo);
        solicitud.setEstado(estado);
        solicitud.setDireccionSolicitada(direccion);
        return solicitud;
    }
}
