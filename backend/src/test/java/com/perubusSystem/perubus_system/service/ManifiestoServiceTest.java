package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Manifiesto;
import com.perubusSystem.perubus_system.dto.ManifiestoSolicitud;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.repository.ManifiestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManifiestoServiceTest {

    private final ManifiestoRepository manifiestoRepository = mock(ManifiestoRepository.class);
    private final EncomiendaRepository encomiendaRepository = mock(EncomiendaRepository.class);
    private final EventoTrackingRepository eventoTrackingRepository = mock(EventoTrackingRepository.class);
    private final ManifiestoService service = new ManifiestoService();
    private final List<Encomienda> asignadas = new ArrayList<>();
    private Manifiesto manifiesto;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "manifiestoRepository", manifiestoRepository);
        ReflectionTestUtils.setField(service, "encomiendaRepository", encomiendaRepository);
        ReflectionTestUtils.setField(service, "eventoTrackingRepository", eventoTrackingRepository);

        manifiesto = new Manifiesto();
        manifiesto.setIdManifiesto(1L);
        manifiesto.setNumBus("BUS-01");
        manifiesto.setRuta("Lima-Ica");
        manifiesto.setFechaViaje(LocalDate.of(2027, 1, 1));
        manifiesto.setCapacidadKg(100.0);
        when(manifiestoRepository.findById(1L)).thenReturn(Optional.of(manifiesto));
        when(encomiendaRepository.findByManifiesto(manifiesto)).thenReturn(asignadas);
    }

    private Encomienda encomienda(String tracking, double peso, String estado) {
        return encomienda(tracking, peso, estado, "Lima-Ica");
    }

    private Encomienda encomienda(String tracking, double peso, String estado, String ruta) {
        Encomienda e = new Encomienda();
        e.setCodigoTracking(tracking);
        e.setRuta(ruta);
        e.setPeso(peso);
        e.setEstadoLogistico(estado);
        when(encomiendaRepository.findByCodigoTracking(tracking)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    void asignaEncomiendasYPasaAEnTransito() {
        Encomienda e = encomienda("PERU-AAAAAAAA", 40.0, "En origen");
        // simula la BD: tras guardar, la encomienda aparece en el manifiesto
        when(encomiendaRepository.saveAll(any())).thenAnswer(inv -> {
            asignadas.addAll(inv.<List<Encomienda>>getArgument(0));
            return inv.getArgument(0);
        });

        var respuesta = service.asignarEncomiendas(1L, List.of("peru-aaaaaaaa"));

        assertThat(e.getEstadoLogistico()).isEqualTo("En tránsito");
        assertThat(e.getManifiesto()).isSameAs(manifiesto);
        assertThat(manifiesto.getPesoTotalCarga()).isEqualTo(40.0);
        assertThat(respuesta.encomiendas()).hasSize(1);
        assertThat(respuesta.encomiendas().get(0).ruta()).isEqualTo("Lima-Ica");
    }

    @Test
    void bloqueaSiSuperaLaCapacidad() {
        encomienda("PERU-BBBBBBBB", 150.0, "En origen");

        assertThatThrownBy(() -> service.asignarEncomiendas(1L, List.of("PERU-BBBBBBBB")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exceso de capacidad");
        verify(encomiendaRepository, never()).saveAll(any());
    }

    @Test
    void rechazaEncomiendaQueNoEstaEnOrigen() {
        encomienda("PERU-CCCCCCCC", 10.0, "Pendiente de Pago");

        assertThatThrownBy(() -> service.asignarEncomiendas(1L, List.of("PERU-CCCCCCCC")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pendiente de Pago");
    }

    @Test
    void rechazaEncomiendaDeOtraRuta() {
        encomienda("PERU-DDDDDDDD", 10.0, "En origen", "Ica-Nazca");

        assertThatThrownBy(() -> service.asignarEncomiendas(1L, List.of("PERU-DDDDDDDD")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ica-Nazca")
                .hasMessageContaining("Lima-Ica");
        verify(encomiendaRepository, never()).saveAll(any());
    }

    @Test
    void rechazaEncomiendaSinRuta() {
        encomienda("PERU-EEEEEEEE", 10.0, "En origen", null);

        assertThatThrownBy(() -> service.asignarEncomiendas(1L, List.of("PERU-EEEEEEEE")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene ruta");
        verify(encomiendaRepository, never()).saveAll(any());
    }

    @Test
    void aceptaLaMismaRutaSinImportarMayusculasNiEspacios() {
        Encomienda e = encomienda("PERU-FFFFFFFF", 10.0, "En origen", "  lima-ICA ");

        service.asignarEncomiendas(1L, List.of("PERU-FFFFFFFF"));

        assertThat(e.getEstadoLogistico()).isEqualTo("En tránsito");
    }

    @Test
    void rechazaListaVacia() {
        assertThatThrownBy(() -> service.asignarEncomiendas(1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualizaDatosYNormalizaElNumeroDeBus() {
        ManifiestoSolicitud solicitud = new ManifiestoSolicitud(
                " bus-22 ", "Lima-Ica", manifiesto.getFechaViaje(), 120.0);
        when(manifiestoRepository.save(manifiesto)).thenReturn(manifiesto);

        var respuesta = service.actualizar(1L, solicitud);

        assertThat(manifiesto.getNumBus()).isEqualTo("BUS-22");
        assertThat(manifiesto.getCapacidadKg()).isEqualTo(120.0);
        assertThat(manifiesto.getPesoTotalCarga()).isZero();
        assertThat(respuesta.numBus()).isEqualTo("BUS-22");
        verify(manifiestoRepository).save(manifiesto);
    }

    @Test
    void rechazaCapacidadMenorALaCargaAsignada() {
        asignadas.add(encomienda("PERU-AAAAAA01", 40.0, "En tránsito"));

        assertThatThrownBy(() -> service.actualizar(1L,
                new ManifiestoSolicitud("BUS-02", "Lima-Ica", manifiesto.getFechaViaje(), 39.0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("40.00 kg");
        verify(manifiestoRepository, never()).save(manifiesto);
    }

    @Test
    void rechazaCambioDeRutaIncompatibleConEncomiendasAsignadas() {
        asignadas.add(encomienda("PERU-AAAAAA02", 20.0, "En tránsito"));

        assertThatThrownBy(() -> service.actualizar(1L,
                new ManifiestoSolicitud("BUS-02", "Ica-Nazca", manifiesto.getFechaViaje(), 100.0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya tiene asignada la encomienda PERU-AAAAAA02")
                .hasMessageContaining("PERU-AAAAAA02")
                .hasMessageContaining("Lima-Ica");
        verify(manifiestoRepository, never()).save(manifiesto);
    }

    @Test
    void permiteCambiarRutaCuandoCoincideConTodasLasEncomiendas() {
        asignadas.add(encomienda("PERU-AAAAAA03", 20.0, "En tránsito", "Ica-Nazca"));

        var respuesta = service.actualizar(1L,
                new ManifiestoSolicitud("BUS-02", "Ica-Nazca", manifiesto.getFechaViaje(), 100.0));

        assertThat(manifiesto.getRuta()).isEqualTo("Ica-Nazca");
        assertThat(respuesta.ruta()).isEqualTo("Ica-Nazca");
    }

    @Test
    void rechazaCapacidadNoPositiva() {
        assertThatThrownBy(() -> service.actualizar(1L,
                new ManifiestoSolicitud("BUS-02", "Lima-Ica", manifiesto.getFechaViaje(), 0.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    void eliminaManifiestoVacio() {
        service.eliminar(1L);

        verify(manifiestoRepository).delete(manifiesto);
    }

    @Test
    void noEliminaManifiestoConEncomiendasAsignadas() {
        asignadas.add(encomienda("PERU-AAAAAA04", 20.0, "Entregado"));

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 encomienda(s) asignada(s)");
        verify(manifiestoRepository, never()).delete(manifiesto);
    }
}
