package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Manifiesto;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.ManifiestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    private final ManifiestoService service = new ManifiestoService();
    private final List<Encomienda> asignadas = new ArrayList<>();
    private Manifiesto manifiesto;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "manifiestoRepository", manifiestoRepository);
        ReflectionTestUtils.setField(service, "encomiendaRepository", encomiendaRepository);

        manifiesto = new Manifiesto();
        manifiesto.setIdManifiesto(1L);
        manifiesto.setNumBus("BUS-01");
        manifiesto.setRuta("Lima-Ica");
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
}
