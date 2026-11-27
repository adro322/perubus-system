package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ComprobanteRespuesta;
import com.perubusSystem.perubus_system.model.Comprobante;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.repository.ComprobanteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.seguridad.Rol;
import com.perubusSystem.perubus_system.service.EncomiendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RequiereRol(Rol.AGENTE)
@RestController
@RequestMapping("/api/comprobantes")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class ComprobanteController {

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @Autowired
    private EncomiendaRepository encomiendaRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<ComprobanteRespuesta> listarComprobantes() {
        return comprobanteRepository.findAllByOrderByFechaPagoDesc().stream()
                .map(ComprobanteRespuesta::desde)
                .toList();
    }

    @GetMapping("/encomienda/{codigoTracking}")
    @Transactional
    public ResponseEntity<?> obtenerPorEncomienda(@PathVariable String codigoTracking) {
        String tracking = codigoTracking == null ? "" : codigoTracking.trim().toUpperCase(Locale.ROOT);
        Optional<Comprobante> existente = comprobanteRepository.findByEncomienda_CodigoTracking(tracking);
        if (existente.isPresent()) {
            return ResponseEntity.ok(ComprobanteRespuesta.desde(existente.get()));
        }

        Optional<Encomienda> encOpt = encomiendaRepository.findByCodigoTracking(tracking);
        if (encOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró la encomienda."));
        }
        Encomienda encomienda = encOpt.get();
        String estado = encomienda.getEstadoLogistico() == null ? "" : encomienda.getEstadoLogistico();
        if (EncomiendaService.ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(estado)
                || "Cancelado".equalsIgnoreCase(estado)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "La encomienda aún no cuenta con un comprobante de pago emitido."));
        }

        // Respaldo para encomiendas pagadas antes de que se guardara el comprobante completo
        BigDecimal subtotal = BigDecimal.valueOf(encomienda.getTarifaBase() == null ? 0.0 : encomienda.getTarifaBase())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv);
        String doc = encomienda.getRemitente() != null && encomienda.getRemitente().getNumDocumento() != null
                ? encomienda.getRemitente().getNumDocumento().trim()
                : "";
        String tipo = doc.length() == 11 ? "FACTURA" : "BOLETA";
        LocalDateTime fecha = encomienda.getFechaRegistro() != null ? encomienda.getFechaRegistro() : LocalDateTime.now();

        Comprobante nuevo = new Comprobante();
        nuevo.setEncomienda(encomienda);
        nuevo.setTipoComprobante(tipo);
        nuevo.setEstadoPago("SIMULADO");
        nuevo.setMontoSubtotal(subtotal);
        nuevo.setMontoIgv(igv);
        nuevo.setMontoTotal(total);
        nuevo.setFechaEmision(fecha);
        nuevo.setFechaPago(fecha);
        Comprobante guardado = comprobanteRepository.save(nuevo);
        if (guardado.getIdComprobante() != null) {
            guardado.setNumeroSerie(("FACTURA".equals(tipo) ? "F001-" : "B001-")
                    + String.format(Locale.ROOT, "%06d", guardado.getIdComprobante()));
            comprobanteRepository.save(guardado);
        }
        return ResponseEntity.ok(ComprobanteRespuesta.desde(guardado));
    }

    @PostMapping
    public Comprobante registrarComprobante(@RequestBody Comprobante comprobante) {
        return comprobanteRepository.save(comprobante);
    }
}