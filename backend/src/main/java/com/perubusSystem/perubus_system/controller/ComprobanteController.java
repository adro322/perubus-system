package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.model.Comprobante;
import com.perubusSystem.perubus_system.repository.ComprobanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comprobantes")
@CrossOrigin(origins = "*") // Para que React no tenga problemas de CORS
public class ComprobanteController {

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @GetMapping
    public List<Comprobante> listarComprobantes() {
        return comprobanteRepository.findAll();
    }

    @PostMapping
    public Comprobante registrarComprobante(@RequestBody Comprobante comprobante) {
        // Aquí luego le podemos agregar la lógica para calcular el IGV si lo necesitan
        return comprobanteRepository.save(comprobante);
    }
}