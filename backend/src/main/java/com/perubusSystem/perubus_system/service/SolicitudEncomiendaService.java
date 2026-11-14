package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.dto.DecisionSolicitudEncomienda;
import com.perubusSystem.perubus_system.dto.SolicitudEncomiendaRespuesta;
import com.perubusSystem.perubus_system.dto.SolicitudEncomiendaSolicitud;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.BultoEncomienda;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.EventoTracking;
import com.perubusSystem.perubus_system.model.SolicitudEncomienda;
import com.perubusSystem.perubus_system.model.Usuario;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.BultoEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.repository.SolicitudEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SolicitudEncomiendaService {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APLICADA = "APLICADA";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String PENDIENTE_PAGO = "PENDIENTE_PAGO";
    private static final String CANCELACION = "CANCELACION";
    private static final String CAMBIO_DIRECCION = "CAMBIO_DIRECCION";
    private static final String AGREGAR_BULTO = "AGREGAR_BULTO";

    private final ClienteRepository clienteRepository;
    private final BultoEncomiendaRepository bultoRepository;
    private final EncomiendaRepository encomiendaRepository;
    private final EventoTrackingRepository eventoTrackingRepository;
    private final SolicitudEncomiendaRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final EncomiendaService encomiendaService;

    public SolicitudEncomiendaService(
            ClienteRepository clienteRepository,
            BultoEncomiendaRepository bultoRepository,
            EncomiendaRepository encomiendaRepository,
            EventoTrackingRepository eventoTrackingRepository,
            SolicitudEncomiendaRepository solicitudRepository,
            UsuarioRepository usuarioRepository,
            EncomiendaService encomiendaService) {
        this.clienteRepository = clienteRepository;
        this.bultoRepository = bultoRepository;
        this.encomiendaRepository = encomiendaRepository;
        this.eventoTrackingRepository = eventoTrackingRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.encomiendaService = encomiendaService;
    }

    @Transactional(readOnly = true)
    public List<SolicitudEncomiendaRespuesta> listarDelCliente(Long clienteId) {
        return solicitudRepository.findByCliente_IdClienteOrderByFechaSolicitudDesc(clienteId).stream()
                .map(SolicitudEncomiendaRespuesta::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudEncomiendaRespuesta> listarPendientes(Long usuarioId) {
        validarPersonal(usuarioId);
        Map<Long, SolicitudEncomienda> solicitudesVisibles = new LinkedHashMap<>();
        solicitudRepository.findByEstadoOrderByFechaSolicitudAsc(PENDIENTE)
                .forEach(solicitud -> solicitudesVisibles.put(solicitud.getIdSolicitud(), solicitud));
        solicitudRepository.findByTipoOrderByFechaSolicitudDesc(AGREGAR_BULTO)
                .forEach(solicitud -> solicitudesVisibles.putIfAbsent(solicitud.getIdSolicitud(), solicitud));
        return solicitudesVisibles.values().stream()
                .sorted((primera, segunda) -> segunda.getFechaSolicitud().compareTo(primera.getFechaSolicitud()))
                .map(SolicitudEncomiendaRespuesta::desde)
                .toList();
    }

    @Transactional
    public SolicitudEncomiendaRespuesta crear(
            Long clienteId,
            String codigoTracking,
            SolicitudEncomiendaSolicitud datos) {
        if (datos == null || datos.tipo() == null) {
            throw new IllegalArgumentException("Indica si solicitas cancelación, cambio de dirección o agregar un bulto.");
        }
        String tipo = datos.tipo().trim().toUpperCase(Locale.ROOT);
        if (!CANCELACION.equals(tipo) && !CAMBIO_DIRECCION.equals(tipo) && !AGREGAR_BULTO.equals(tipo)) {
            throw new IllegalArgumentException("El tipo de solicitud no es válido.");
        }
        if (CAMBIO_DIRECCION.equals(tipo)) {
            validarDireccion(datos.direccionDestinatario());
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el cliente."));
        Encomienda encomienda = encomiendaRepository.findByCodigoTracking(codigoTracking)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la encomienda."));
        if (encomienda.getRemitente() == null
                || !encomienda.getRemitente().getIdCliente().equals(clienteId)) {
            throw new IllegalStateException("Solo el remitente puede solicitar cambios en esta encomienda.");
        }
        validarNoTerminal(encomienda);
        if (solicitudRepository.existsByEncomienda_IdEncomiendaAndEstadoIn(
                encomienda.getIdEncomienda(), List.of(PENDIENTE, PENDIENTE_PAGO))) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para esta encomienda.");
        }

        SolicitudEncomienda solicitud = new SolicitudEncomienda();
        solicitud.setCliente(cliente);
        solicitud.setEncomienda(encomienda);
        solicitud.setTipo(tipo);
        solicitud.setDireccionSolicitada(CAMBIO_DIRECCION.equals(tipo)
                ? datos.direccionDestinatario().trim()
                : null);
        if (AGREGAR_BULTO.equals(tipo)) {
            validarBulto(datos.descripcionBulto(), datos.pesoBulto());
            solicitud.setDescripcionBulto(datos.descripcionBulto().trim());
            solicitud.setPesoBulto(datos.pesoBulto());
        }

        String estadoActual = encomienda.getEstadoLogistico();
        boolean pendienteDePago = EncomiendaService.ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(estadoActual);
        boolean enOrigen = EncomiendaService.ESTADO_EN_ORIGEN.equalsIgnoreCase(estadoActual);
        boolean antesDelDespacho = encomienda.getManifiesto() == null && (pendienteDePago || enOrigen);
        if (!antesDelDespacho) {
            throw new IllegalStateException("Solo se pueden solicitar cambios antes de que la encomienda salga de origen.");
        }

        if (CAMBIO_DIRECCION.equals(tipo) && pendienteDePago) {
            String direccionAnterior = encomienda.getDireccionDestinatario();
            if (direccionAnterior != null
                    && direccionAnterior.trim().equalsIgnoreCase(solicitud.getDireccionSolicitada())) {
                throw new IllegalArgumentException("La nueva dirección debe ser diferente a la dirección actual.");
            }
            encomienda.setDireccionDestinatario(solicitud.getDireccionSolicitada());
            encomiendaRepository.save(encomienda);
            solicitud.setEstado(APLICADA);
            solicitud.setRespuesta("Cambio aplicado automáticamente antes del pago y del despacho.");
            registrarEvento(encomienda, "Dirección del destinatario actualizada a solicitud del remitente. Dirección anterior: "
                    + (direccionAnterior == null ? "no registrada" : direccionAnterior) + ".", null);
        } else {
            if (CAMBIO_DIRECCION.equals(tipo) && enOrigen) {
                String direccionActual = encomienda.getDireccionDestinatario();
                if (direccionActual != null
                        && direccionActual.trim().equalsIgnoreCase(solicitud.getDireccionSolicitada())) {
                    throw new IllegalArgumentException("La nueva dirección debe ser diferente a la dirección actual.");
                }
            } else if (CANCELACION.equals(tipo)) {
                solicitud.setRespuesta(pendienteDePago
                        ? "Pendiente de aprobación del personal. La encomienda seguirá registrada hasta que se resuelva la solicitud."
                        : "Encomienda pagada: requiere aprobación del personal. El reembolso no es automático.");
                solicitud.setEstado(PENDIENTE);
                return SolicitudEncomiendaRespuesta.desde(solicitudRepository.save(solicitud));
            } else if (AGREGAR_BULTO.equals(tipo) && (enOrigen || pendienteDePago)) {
                solicitud.setEstado(PENDIENTE);
                solicitud.setRespuesta(pendienteDePago
                        ? "Pendiente de aprobación del personal. Si se aprueba, el bulto se agregará y se recalculará la tarifa antes del pago."
                        : "Pendiente de revisión del personal. Si se aprueba, se calculará el importe adicional y deberá pagarse antes del despacho.");
                return SolicitudEncomiendaRespuesta.desde(solicitudRepository.save(solicitud));
            } else {
                throw new IllegalStateException("La solicitud no está disponible para el estado actual de la encomienda.");
            }

            solicitud.setEstado(PENDIENTE);
            solicitud.setRespuesta("Pendiente de aprobación del personal porque el envío ya fue pagado.");
        }

        return SolicitudEncomiendaRespuesta.desde(solicitudRepository.save(solicitud));
    }

    @Transactional
    public SolicitudEncomiendaRespuesta resolver(
            Long solicitudId,
            Long usuarioId,
            DecisionSolicitudEncomienda decision) {
        validarPersonal(usuarioId);
        if (decision == null || decision.aprobar() == null) {
            throw new IllegalArgumentException("Indica si apruebas o rechazas la solicitud.");
        }
        String comentario = decision.comentario() == null ? "" : decision.comentario().trim();
        if (!decision.aprobar() && comentario.isBlank()) {
            throw new IllegalArgumentException("Indica el motivo del rechazo.");
        }
        if (comentario.length() > 500) {
            throw new IllegalArgumentException("El comentario no puede superar 500 caracteres.");
        }

        SolicitudEncomienda solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la solicitud."));
        if (!PENDIENTE.equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue atendida.");
        }
        Encomienda encomienda = solicitud.getEncomienda();
        validarNoTerminal(encomienda);

        if (decision.aprobar()) {
            if (CANCELACION.equals(solicitud.getTipo())) {
                validarCancelacionPermitida(encomienda);
                encomienda.setEstadoLogistico("Cancelado");
                encomiendaRepository.save(encomienda);
                solicitud.setEstado(APROBADA);
                solicitud.setRespuesta(comentario.isBlank()
                        ? "Cancelación aprobada por el personal. Coordinar el tratamiento del pago si corresponde."
                        : comentario);
                registrarEvento(encomienda, "Cancelación aprobada por el personal. "
                        + (comentario.isBlank() ? "Coordinar el tratamiento del pago si corresponde." : comentario), null);
            } else if (CAMBIO_DIRECCION.equals(solicitud.getTipo())) {
                if (!EncomiendaService.ESTADO_EN_ORIGEN.equalsIgnoreCase(encomienda.getEstadoLogistico())
                        || encomienda.getManifiesto() != null) {
                    throw new IllegalStateException(
                            "El cambio de dirección ya no puede aprobarse porque la encomienda salió de origen.");
                }
                validarDireccion(solicitud.getDireccionSolicitada());
                String direccionAnterior = encomienda.getDireccionDestinatario();
                if (direccionAnterior != null
                        && direccionAnterior.trim().equalsIgnoreCase(solicitud.getDireccionSolicitada().trim())) {
                    throw new IllegalStateException("La dirección solicitada ya coincide con la dirección actual.");
                }
                encomienda.setDireccionDestinatario(solicitud.getDireccionSolicitada().trim());
                encomiendaRepository.save(encomienda);
                solicitud.setEstado(APROBADA);
                solicitud.setRespuesta(comentario.isBlank()
                        ? "Cambio de dirección aprobado por el personal."
                        : comentario);
                registrarEvento(encomienda, "Cambio de dirección aprobado por el personal. Dirección anterior: "
                        + (direccionAnterior == null ? "no registrada" : direccionAnterior) + ".", null);
            } else if (AGREGAR_BULTO.equals(solicitud.getTipo())) {
                boolean pendienteDePago = EncomiendaService.ESTADO_PENDIENTE_PAGO
                        .equalsIgnoreCase(encomienda.getEstadoLogistico());
                boolean enOrigen = EncomiendaService.ESTADO_EN_ORIGEN
                        .equalsIgnoreCase(encomienda.getEstadoLogistico());
                if ((!pendienteDePago && !enOrigen) || encomienda.getManifiesto() != null) {
                    throw new IllegalStateException(
                            "No se puede agregar el bulto porque la encomienda ya salió de origen.");
                }
                validarBulto(solicitud.getDescripcionBulto(), solicitud.getPesoBulto());
                BigDecimal tarifaAnterior = BigDecimal.valueOf(encomienda.getTarifaBase())
                        .setScale(2, RoundingMode.HALF_UP);
                aplicarBulto(encomienda, solicitud);
                BigDecimal tarifaNueva = BigDecimal.valueOf(encomienda.getTarifaBase())
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal diferenciaTarifa = tarifaNueva.subtract(tarifaAnterior).max(BigDecimal.ZERO);
                BigDecimal montoAdicional = pendienteDePago ? BigDecimal.ZERO : diferenciaTarifa;
                solicitud.setMontoAdicional(montoAdicional);
                if (pendienteDePago) {
                    solicitud.setEstado(APROBADA);
                    solicitud.setRespuesta(comentario.isBlank()
                            ? "Bulto aprobado y agregado antes del pago. La tarifa total fue actualizada."
                            : comentario);
                } else if (montoAdicional.signum() > 0) {
                    encomienda.setEstadoLogistico("Pendiente de pago adicional");
                    solicitud.setEstado(PENDIENTE_PAGO);
                    solicitud.setRespuesta("Bulto aprobado. Paga el importe adicional antes de asignar el envío a un manifiesto.");
                } else {
                    solicitud.setEstado(APROBADA);
                    solicitud.setRespuesta("Bulto aprobado. No se generó un importe adicional.");
                }
                encomiendaRepository.save(encomienda);
                registrarEvento(encomienda, (pendienteDePago
                        ? "Bulto adicional aprobado y agregado antes del pago: "
                        : "Bulto adicional aprobado: ") + solicitud.getDescripcionBulto()
                        + " (" + solicitud.getPesoBulto() + " kg)."
                        + (pendienteDePago
                                ? " Tarifa total actualizada antes del pago."
                                : montoAdicional.signum() > 0
                                        ? " Pendiente de pago adicional: S/ " + montoAdicional.toPlainString() + "."
                                        : " Sin cargo adicional."),
                        null);
                return SolicitudEncomiendaRespuesta.desde(solicitudRepository.save(solicitud));
            }
        } else {
            solicitud.setEstado(RECHAZADA);
            solicitud.setRespuesta(comentario);
            registrarEvento(encomienda, "Solicitud de "
                    + (CANCELACION.equals(solicitud.getTipo())
                            ? "cancelación"
                            : CAMBIO_DIRECCION.equals(solicitud.getTipo()) ? "cambio de dirección" : "bulto adicional")
                    + " rechazada por el personal: " + comentario, null);
        }
        return SolicitudEncomiendaRespuesta.desde(solicitudRepository.save(solicitud));
    }

    @Transactional
    public SolicitudEncomiendaRespuesta pagarImporteAdicional(Long clienteId, Long solicitudId) {
        SolicitudEncomienda solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la solicitud."));
        if (solicitud.getCliente() == null || !solicitud.getCliente().getIdCliente().equals(clienteId)) {
            throw new IllegalStateException("Solo el remitente puede pagar este importe adicional.");
        }
        if (!AGREGAR_BULTO.equals(solicitud.getTipo()) || !PENDIENTE_PAGO.equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud no tiene un importe adicional pendiente de pago.");
        }
        Encomienda encomienda = solicitud.getEncomienda();
        if (!"Pendiente de pago adicional".equalsIgnoreCase(encomienda.getEstadoLogistico())) {
            throw new IllegalStateException("El envío no está pendiente del pago adicional de este bulto.");
        }
        solicitud.setEstado(APROBADA);
        solicitud.setFechaPagoAdicional(java.time.LocalDateTime.now());
        solicitud.setRespuesta("Pago adicional simulado y confirmado.");
        encomienda.setEstadoLogistico(EncomiendaService.ESTADO_EN_ORIGEN);
        encomiendaRepository.save(encomienda);
        registrarEvento(encomienda, "Pago adicional simulado confirmado por el bulto '"
                + solicitud.getDescripcionBulto() + "'. El envío puede asignarse a un manifiesto.", null);
        return SolicitudEncomiendaRespuesta.desde(solicitudRepository.save(solicitud));
    }

    private void validarPersonal(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el usuario."));
        if (!Boolean.TRUE.equals(usuario.getEstado())
                || !("ADMIN".equalsIgnoreCase(usuario.getRol()) || "AGENTE".equalsIgnoreCase(usuario.getRol()))) {
            throw new IllegalStateException("No tienes permiso para gestionar solicitudes.");
        }
    }

    private void validarNoTerminal(Encomienda encomienda) {
        String estado = encomienda.getEstadoLogistico() == null ? "" : encomienda.getEstadoLogistico();
        if ("Cancelado".equalsIgnoreCase(estado)
                || EncomiendaService.ESTADO_ENTREGADO.equalsIgnoreCase(estado)
                || EncomiendaService.ESTADO_RECOGIDO.equalsIgnoreCase(estado)) {
            throw new IllegalStateException("No se pueden solicitar cambios para una encomienda cancelada o finalizada.");
        }
    }

    private void validarCancelacionPermitida(Encomienda encomienda) {
        String estado = encomienda.getEstadoLogistico();
        boolean pendienteDePago = EncomiendaService.ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(estado);
        boolean enOrigen = EncomiendaService.ESTADO_EN_ORIGEN.equalsIgnoreCase(estado);
        if ((!pendienteDePago && !enOrigen) || encomienda.getManifiesto() != null) {
            throw new IllegalStateException(
                    "La cancelación ya no puede aprobarse porque la encomienda salió de origen.");
        }
    }

    private void validarDireccion(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La nueva dirección es obligatoria.");
        }
        if (direccion.trim().length() > 500) {
            throw new IllegalArgumentException("La dirección no puede superar 500 caracteres.");
        }
    }

    private void validarBulto(String descripcion, Double peso) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del bulto es obligatoria.");
        }
        if (descripcion.trim().length() > 250) {
            throw new IllegalArgumentException("La descripción del bulto no puede superar 250 caracteres.");
        }
        if (peso == null || !Double.isFinite(peso) || peso <= 0 || peso > 9_999_999.99) {
            throw new IllegalArgumentException("El peso del bulto debe ser mayor a 0 y no superar 9,999,999.99 kg.");
        }
    }

    private void aplicarBulto(Encomienda encomienda, SolicitudEncomienda solicitud) {
        BultoEncomienda bulto = new BultoEncomienda();
        bulto.setEncomienda(encomienda);
        bulto.setDescripcion(solicitud.getDescripcionBulto().trim());
        bulto.setPeso(solicitud.getPesoBulto());

        if (encomienda.getPesoOriginal() == null) {
            encomienda.setPesoOriginal(encomienda.getPeso());
        }
        double pesoTotal = encomienda.getPeso() + solicitud.getPesoBulto();
        BigDecimal tarifa = encomiendaService.calcularTarifaRuta(encomienda.getRuta(), pesoTotal);
        encomienda.setPeso(pesoTotal);
        encomienda.setTarifaBase(tarifa.doubleValue());
        bultoRepository.save(bulto);
        encomiendaRepository.save(encomienda);
    }

    private void registrarEvento(Encomienda encomienda, String detalle, String ubicacion) {
        EventoTracking evento = new EventoTracking();
        evento.setEncomienda(encomienda);
        evento.setEstado(encomienda.getEstadoLogistico());
        evento.setDetalle(detalle);
        evento.setUbicacion(ubicacion);
        eventoTrackingRepository.save(evento);
    }
}
