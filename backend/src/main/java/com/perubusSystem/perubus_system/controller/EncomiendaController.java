package com.perubusSystem.perubus_system.Controller;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.model.Encomienda;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/encomiendas")
@CrossOrigin(origins = "*")
public class EncomiendaController {

    @Autowired
    private EncomiendaRepository encomiendaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    // ==========================================
    // CUS-02: COTIZAR ENVÍO (Solo calcula, no guarda en BD)
    // ==========================================
    @PostMapping("/cotizar")
    public ResponseEntity<?> cotizarEnvio(@RequestBody Map<String, Object> datosCotizacion) {
        // Obtenemos el peso enviado desde React
        Double peso = Double.valueOf(datosCotizacion.get("peso").toString());

        // Reglas de negocio de PeruBus
        double tarifaBaseMinima = 15.0; // Lo mínimo que se cobra por cualquier envío pequeño
        double costoPorKilo = 5.5; // Costo por kilo

        // El sistema calcula el costo por peso, pero si es menor al mínimo, aplica la
        // base.
        double costoEstimado = Math.max(tarifaBaseMinima, peso * costoPorKilo);

        return ResponseEntity.ok(Map.of("costoEstimado", costoEstimado));
    }

    // ==========================================
    // CUS-01: REGISTRAR ENCOMIENDA (Sí guarda en BD)
    // ==========================================
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarEncomienda(@RequestBody Encomienda encomienda) {

        // Validamos y guardamos el Cliente (Si no existe, se crea)
        if (encomienda.getCliente() != null) {
            Cliente cliente = clienteRepository.findByNumDocumento(encomienda.getCliente().getNumDocumento())
                    .orElseGet(() -> clienteRepository.save(encomienda.getCliente()));
            encomienda.setCliente(cliente);
        }
        // Generar Código de Tracking automático
        String tracking = "PERU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        encomienda.setCodigoTracking(tracking);

        encomienda.setEstado("En origen");

        Encomienda encomiendaGuardada = encomiendaRepository.save(encomienda);

        return ResponseEntity.ok(encomiendaGuardada);
    }

    // Para ver el historial de encomiendas
    @GetMapping
    public ResponseEntity<?> listarEncomiendas() {
        return ResponseEntity.ok(encomiendaRepository.findAll());
    }
}