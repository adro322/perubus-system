package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.ComprobanteRespuesta;
import com.perubusSystem.perubus_system.dto.EstadisticasRespuesta;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Comprobante;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.model.Incidencia;
import com.perubusSystem.perubus_system.model.Manifiesto;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.ComprobanteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.IncidenciaRepository;
import com.perubusSystem.perubus_system.repository.ManifiestoRepository;
import com.perubusSystem.perubus_system.repository.SolicitudEncomiendaRepository;
import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.service.EncomiendaService;
import com.perubusSystem.perubus_system.service.SolicitudEncomiendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiereRol({})
@RestController
@RequestMapping("/api/estadisticas")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class EstadisticasController {

    @Autowired
    private EncomiendaRepository encomiendaRepository;

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ManifiestoRepository manifiestoRepository;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private SolicitudEncomiendaRepository solicitudRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public EstadisticasRespuesta obtenerEstadisticas() {
        List<Encomienda> encomiendas = encomiendaRepository.findAll();
        List<Comprobante> comprobantes = comprobanteRepository.findAllByOrderByFechaPagoDesc();
        List<Cliente> clientes = clienteRepository.findAll();
        List<Manifiesto> manifiestos = manifiestoRepository.findAll();
        List<Incidencia> incidencias = incidenciaRepository.findAll();

        long totalEncomiendas = encomiendas.size();
        long entregadas = 0;
        long enCurso = 0;
        long pendientesPago = 0;
        long canceladas = 0;
        double pesoTotalKg = 0.0;

        Map<String, Long> conteoPorEstado = new LinkedHashMap<>();
        Map<String, AcumuladorRuta> mapaRutas = new LinkedHashMap<>();

        for (Encomienda enc : encomiendas) {
            String estado = enc.getEstadoLogistico() == null ? "Sin estado" : enc.getEstadoLogistico().trim();
            conteoPorEstado.merge(estado, 1L, Long::sum);

            boolean esCancelado = "Cancelado".equalsIgnoreCase(estado);
            boolean esPendientePago = EncomiendaService.ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(estado);
            boolean esEntregado = EncomiendaService.ESTADO_ENTREGADO.equalsIgnoreCase(estado)
                    || EncomiendaService.ESTADO_RECOGIDO.equalsIgnoreCase(estado);

            if (esCancelado) {
                canceladas++;
            } else if (esPendientePago) {
                pendientesPago++;
            } else if (esEntregado) {
                entregadas++;
            } else {
                enCurso++;
            }

            double peso = enc.getPeso() == null ? 0.0 : enc.getPeso();
            if (!esCancelado) {
                pesoTotalKg += peso;
            }

            String ruta = (enc.getRuta() == null || enc.getRuta().isBlank())
                    ? "Sin ruta"
                    : enc.getRuta().trim();
            AcumuladorRuta acum = mapaRutas.computeIfAbsent(ruta, k -> new AcumuladorRuta());
            if (!esCancelado) {
                acum.cantidad++;
                acum.pesoKg += peso;
                if (!esPendientePago && enc.getTarifaBase() != null) {
                    BigDecimal sub = BigDecimal.valueOf(enc.getTarifaBase()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal igv = sub.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
                    acum.subtotal = acum.subtotal.add(sub);
                    acum.total = acum.total.add(sub.add(igv));
                }
            }
        }

        // Cálculo de ingresos y comprobantes
        BigDecimal ingresosSubtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ingresosIgv = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ingresosTotales = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long totalBoletas = 0;
        long totalFacturas = 0;

        List<ComprobanteRespuesta> comprobantesDto = comprobantes.stream()
                .map(ComprobanteRespuesta::desde)
                .toList();

        for (ComprobanteRespuesta comp : comprobantesDto) {
            if (comp.montoSubtotal() != null) {
                ingresosSubtotal = ingresosSubtotal.add(comp.montoSubtotal());
            }
            if (comp.montoIgv() != null) {
                ingresosIgv = ingresosIgv.add(comp.montoIgv());
            }
            if (comp.montoTotal() != null) {
                ingresosTotales = ingresosTotales.add(comp.montoTotal());
            }
            if ("FACTURA".equalsIgnoreCase(comp.tipoComprobante())) {
                totalFacturas++;
            } else {
                totalBoletas++;
            }
        }

        // Si existen encomiendas pagadas sin fila en comprobante (datos antiguos), sumarlas para no subestimar ingresos
        if (comprobantesDto.isEmpty()) {
            for (AcumuladorRuta ar : mapaRutas.values()) {
                ingresosSubtotal = ingresosSubtotal.add(ar.subtotal);
                ingresosTotales = ingresosTotales.add(ar.total);
            }
            ingresosIgv = ingresosTotales.subtract(ingresosSubtotal).max(BigDecimal.ZERO);
        }

        long totalClientes = clientes.size();
        long clientesConPortalActivo = clientes.stream()
                .filter(c -> c.tieneCuentaPortal() && !c.cuentaBloqueada())
                .count();

        long totalManifiestos = manifiestos.size();
        double sumaOcupacion = 0.0;
        int manifiestosConCapacidad = 0;
        for (Manifiesto m : manifiestos) {
            if (m.getCapacidadKg() != null && m.getCapacidadKg() > 0) {
                double carga = m.getPesoTotalCarga() == null ? 0.0 : m.getPesoTotalCarga();
                sumaOcupacion += Math.min(100.0, (carga / m.getCapacidadKg()) * 100.0);
                manifiestosConCapacidad++;
            }
        }
        double ocupacionPromedio = manifiestosConCapacidad == 0
                ? 0.0
                : Math.round((sumaOcupacion / manifiestosConCapacidad) * 10.0) / 10.0;

        long incidenciasAbiertas = incidencias.stream()
                .filter(i -> !"RESUELTA".equalsIgnoreCase(i.getEstado()))
                .count();
        long incidenciasResueltas = incidencias.size() - incidenciasAbiertas;

        long solicitudesPendientes = solicitudRepository
                .findByEstadoOrderByFechaSolicitudAsc(SolicitudEncomiendaService.PENDIENTE)
                .size();

        List<EstadisticasRespuesta.ItemEstado> porEstado = new ArrayList<>();
        for (Map.Entry<String, Long> entry : conteoPorEstado.entrySet()) {
            double pct = totalEncomiendas == 0
                    ? 0.0
                    : Math.round((entry.getValue() * 1000.0) / totalEncomiendas) / 10.0;
            porEstado.add(new EstadisticasRespuesta.ItemEstado(entry.getKey(), entry.getValue(), pct));
        }
        porEstado.sort(Comparator.comparingLong(EstadisticasRespuesta.ItemEstado::cantidad).reversed());

        List<EstadisticasRespuesta.ItemRuta> porRuta = new ArrayList<>();
        for (Map.Entry<String, AcumuladorRuta> entry : mapaRutas.entrySet()) {
            AcumuladorRuta ar = entry.getValue();
            porRuta.add(new EstadisticasRespuesta.ItemRuta(
                    entry.getKey(),
                    ar.cantidad,
                    Math.round(ar.pesoKg * 100.0) / 100.0,
                    ar.subtotal,
                    ar.total));
        }
        porRuta.sort(Comparator.comparingLong(EstadisticasRespuesta.ItemRuta::cantidadEncomiendas).reversed());

        List<ComprobanteRespuesta> ultimosComprobantes = comprobantesDto.stream()
                .limit(25)
                .toList();

        return new EstadisticasRespuesta(
                totalEncomiendas,
                entregadas,
                enCurso,
                pendientesPago,
                canceladas,
                Math.round(pesoTotalKg * 100.0) / 100.0,
                ingresosSubtotal,
                ingresosIgv,
                ingresosTotales,
                comprobantesDto.size(),
                totalBoletas,
                totalFacturas,
                totalClientes,
                clientesConPortalActivo,
                totalManifiestos,
                ocupacionPromedio,
                incidenciasAbiertas,
                incidenciasResueltas,
                solicitudesPendientes,
                porEstado,
                porRuta,
                ultimosComprobantes);
    }

    private static class AcumuladorRuta {
        long cantidad = 0;
        double pesoKg = 0.0;
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}

