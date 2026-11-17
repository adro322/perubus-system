package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.seguridad.Rol;
import com.perubusSystem.perubus_system.model.Comprobante;
import com.perubusSystem.perubus_system.repository.ComprobanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiereRol(Rol.AGENTE)
@RestController
@RequestMapping("/api/comprobantes")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
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