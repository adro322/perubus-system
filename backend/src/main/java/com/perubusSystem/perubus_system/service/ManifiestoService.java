package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.dto.ManifiestoRespuesta;
import com.perubusSystem.perubus_system.dto.ManifiestoSolicitud;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.EventoTracking;
import com.perubusSystem.perubus_system.model.Manifiesto;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
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
    @Autowired
    private EventoTrackingRepository eventoTrackingRepository;

    public Manifiesto crear(ManifiestoSolicitud datos) {
        validarDatos(datos);

        Manifiesto m = new Manifiesto();
        m.setNumBus(datos.numBus().trim().toUpperCase(Locale.ROOT));
        m.setRuta(datos.ruta().trim());
        m.setFechaViaje(datos.fechaViaje());
        m.setCapacidadKg(datos.capacidadKg());
        m.setPesoTotalCarga(0.0);
        return manifiestoRepository.save(m);
    }

    @Transactional
    public ManifiestoRespuesta actualizar(Long id, ManifiestoSolicitud datos) {
        validarDatos(datos);
        Manifiesto manifiesto = buscar(id);
        List<Encomienda> encomiendas = encomiendaRepository.findByManifiesto(manifiesto);
        double pesoAsignado = encomiendas.stream()
                .map(Encomienda::getPeso)
                .filter(peso -> peso != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        if (datos.capacidadKg() < pesoAsignado) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "La capacidad no puede ser menor a la carga asignada (%.2f kg).", pesoAsignado));
        }

        String rutaNueva = datos.ruta().trim();
        for (Encomienda encomienda : encomiendas) {
            if (encomienda.getRuta() == null || !encomienda.getRuta().trim().equalsIgnoreCase(rutaNueva)) {
                throw new IllegalStateException("No se puede cambiar la ruta del manifiesto a '" + rutaNueva
                        + "' porque ya tiene asignada la encomienda " + encomienda.getCodigoTracking()
                        + ", cuya ruta registrada es '" + encomienda.getRuta() + "'.");
            }
        }

        manifiesto.setNumBus(datos.numBus().trim().toUpperCase(Locale.ROOT));
        manifiesto.setRuta(rutaNueva);
        manifiesto.setFechaViaje(datos.fechaViaje());
        manifiesto.setCapacidadKg(datos.capacidadKg());
        manifiesto.setPesoTotalCarga(pesoAsignado);
        manifiestoRepository.save(manifiesto);
        return aRespuesta(manifiesto);
    }

    @Transactional
    public void eliminar(Long id) {
        Manifiesto manifiesto = buscar(id);
        List<Encomienda> encomiendas = encomiendaRepository.findByManifiesto(manifiesto);
        if (!encomiendas.isEmpty()) {
            throw new IllegalStateException("No se puede eliminar el manifiesto porque tiene "
                    + encomiendas.size() + " encomienda(s) asignada(s).");
        }
        manifiestoRepository.delete(manifiesto);
    }

    @Transactional(readOnly = true)
    public List<ManifiestoRespuesta> listar() {
        return manifiestoRepository.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ManifiestoRespuesta obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    // CUS-07: asigna encomiendas "En origen" de la misma ruta del bus, valida capacidad
    // y las pasa a "En tránsito"
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
            validarRuta(e, manifiesto);
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
            EventoTracking evento = new EventoTracking();
            evento.setEncomienda(e);
            evento.setEstado(ESTADO_EN_TRANSITO);
            evento.setDetalle("Encomienda asignada al bus " + manifiesto.getNumBus() + " para la ruta "
                    + manifiesto.getRuta() + ".");
            eventoTrackingRepository.save(evento);
        }
        encomiendaRepository.saveAll(nuevas);
        manifiesto.setPesoTotalCarga(pesoActual + pesoNuevo);
        manifiestoRepository.save(manifiesto);
        return aRespuesta(manifiesto);
    }

    // La encomienda solo puede viajar en un bus de su misma ruta (ej. Lima-Ica)
    private void validarRuta(Encomienda e, Manifiesto m) {
        String rutaEncomienda = e.getRuta() == null ? "" : e.getRuta().trim();
        if (rutaEncomienda.isEmpty())
            throw new IllegalStateException("La encomienda " + e.getCodigoTracking() + " no tiene ruta registrada.");
        String rutaManifiesto = m.getRuta() == null ? "" : m.getRuta().trim();
        if (!rutaEncomienda.equalsIgnoreCase(rutaManifiesto))
            throw new IllegalStateException("La encomienda " + e.getCodigoTracking() + " es de la ruta "
                    + rutaEncomienda + " y el manifiesto es de la ruta " + rutaManifiesto + ".");
    }

    private Manifiesto buscar(Long id) {
        return manifiestoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el manifiesto " + id + "."));
    }

    private void validarDatos(ManifiestoSolicitud datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Debe indicar los datos del manifiesto.");
        }
        if (datos.numBus() == null || datos.numBus().isBlank()) {
            throw new IllegalArgumentException("El número de bus es obligatorio.");
        }
        if (datos.numBus().trim().length() > 20) {
            throw new IllegalArgumentException("El número de bus no puede superar 20 caracteres.");
        }
        if (datos.ruta() == null || datos.ruta().isBlank()) {
            throw new IllegalArgumentException("La ruta es obligatoria.");
        }
        if (datos.fechaViaje() == null) {
            throw new IllegalArgumentException("La fecha de viaje es obligatoria.");
        }
        if (datos.capacidadKg() == null || !Double.isFinite(datos.capacidadKg())
                || datos.capacidadKg() <= 0 || datos.capacidadKg() > 99_999_999.99) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a 0 y no superar 99,999,999.99 kg.");
        }
    }

    private double pesoActual(Manifiesto m) {
        return encomiendaRepository.findByManifiesto(m).stream().mapToDouble(Encomienda::getPeso).sum();
    }

    private ManifiestoRespuesta aRespuesta(Manifiesto m) {
        List<ManifiestoRespuesta.Item> items = encomiendaRepository.findByManifiesto(m).stream()
                .map(e -> new ManifiestoRespuesta.Item(
                        e.getCodigoTracking(),
                        e.getDescripcion(),
                        e.getPeso(),
                        nombre(e.getRemitente()),
                        nombre(e.getDestinatario()),
                        e.getEstadoLogistico(),
                        e.getRuta(),
                        e.getDireccionDestinatario()))
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
