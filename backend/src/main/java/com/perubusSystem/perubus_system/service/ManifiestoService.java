package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.dto.ManifiestoRespuesta;
import com.perubusSystem.perubus_system.dto.ManifiestoSolicitud;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Manifiesto;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.ManifiestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class ManifiestoService {

    public static final String ESTADO_ASIGNABLE = "En origen";
    public static final String ESTADO_EN_TRANSITO = "En tránsito";

    @Autowired
    private ManifiestoRepository manifiestoRepository;
    @Autowired
    private EncomiendaRepository encomiendaRepository;

    public Manifiesto crear(ManifiestoSolicitud datos) {
        if (datos.numBus() == null || datos.numBus().isBlank())
            throw new IllegalArgumentException("El número de bus es obligatorio.");
        if (datos.ruta() == null || datos.ruta().isBlank())
            throw new IllegalArgumentException("La ruta es obligatoria.");
        if (datos.fechaViaje() == null)
            throw new IllegalArgumentException("La fecha de viaje es obligatoria.");
        if (datos.capacidadKg() == null || datos.capacidadKg() <= 0)
            throw new IllegalArgumentException("La capacidad del bus debe ser mayor a 0.");

        Manifiesto m = new Manifiesto();
        m.setNumBus(datos.numBus().trim().toUpperCase(Locale.ROOT));
        m.setRuta(datos.ruta().trim());
        m.setFechaViaje(datos.fechaViaje());
        m.setCapacidadKg(datos.capacidadKg());
        m.setPesoTotalCarga(0.0);
        return manifiestoRepository.save(m);
    }

    @Transactional(readOnly = true)
    public List<ManifiestoRespuesta> listar() {
        return manifiestoRepository.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ManifiestoRespuesta obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    // CUS-07: asigna encomiendas "En origen", valida capacidad y las pasa a "En tránsito"
    @Transactional
    public ManifiestoRespuesta asignarEncomiendas(Long id, List<String> trackings) {
        if (trackings == null || trackings.isEmpty())
            throw new IllegalArgumentException("Debe indicar al menos una encomienda.");

        Manifiesto manifiesto = buscar(id);
        double pesoActual = pesoActual(manifiesto);
        double capacidad = manifiesto.getCapacidadKg() == null ? Double.MAX_VALUE : manifiesto.getCapacidadKg();

        List<Encomienda> nuevas = trackings.stream().distinct().map(t -> {
            String codigo = t == null ? "" : t.trim().toUpperCase(Locale.ROOT);
            Encomienda e = encomiendaRepository.findByCodigoTracking(codigo)
                    .orElseThrow(() -> new NoSuchElementException("No existe la encomienda " + codigo + "."));
            if (!ESTADO_ASIGNABLE.equalsIgnoreCase(e.getEstadoLogistico()))
                throw new IllegalStateException("La encomienda " + codigo + " no está en estado '"
                        + ESTADO_ASIGNABLE + "' (estado actual: " + e.getEstadoLogistico() + ").");
            return e;
        }).toList();

        double pesoNuevo = nuevas.stream().mapToDouble(Encomienda::getPeso).sum();
        if (pesoActual + pesoNuevo > capacidad)
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Exceso de capacidad: %.2f kg cargados + %.2f kg nuevos supera los %.2f kg del bus.",
                    pesoActual, pesoNuevo, capacidad));

        for (Encomienda e : nuevas) {
            e.setManifiesto(manifiesto);
            e.setEstadoLogistico(ESTADO_EN_TRANSITO);
        }
        encomiendaRepository.saveAll(nuevas);
        manifiesto.setPesoTotalCarga(pesoActual + pesoNuevo);
        manifiestoRepository.save(manifiesto);
        return aRespuesta(manifiesto);
    }

    private Manifiesto buscar(Long id) {
        return manifiestoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el manifiesto " + id + "."));
    }

    private double pesoActual(Manifiesto m) {
        return encomiendaRepository.findByManifiesto(m).stream().mapToDouble(Encomienda::getPeso).sum();
    }

    private ManifiestoRespuesta aRespuesta(Manifiesto m) {
        List<ManifiestoRespuesta.Item> items = encomiendaRepository.findByManifiesto(m).stream()
                .map(e -> new ManifiestoRespuesta.Item(e.getCodigoTracking(), e.getDescripcion(), e.getPeso(),
                        nombre(e.getRemitente()), nombre(e.getDestinatario()), e.getEstadoLogistico()))
                .toList();
        double total = items.stream().mapToDouble(ManifiestoRespuesta.Item::peso).sum();
        return new ManifiestoRespuesta(m.getIdManifiesto(), m.getNumBus(), m.getRuta(),
                m.getFechaViaje() == null ? null : m.getFechaViaje().toString(),
                m.getCapacidadKg(), total, items);
    }

    private String nombre(Cliente c) {
        if (c == null)
            return "";
        return ((c.getNombres() == null ? "" : c.getNombres()) + " "
                + (c.getApellidos() == null ? "" : c.getApellidos())).trim();
    }
}
