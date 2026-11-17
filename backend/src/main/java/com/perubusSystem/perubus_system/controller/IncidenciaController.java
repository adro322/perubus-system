package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.seguridad.RequiereRol;
import com.perubusSystem.perubus_system.seguridad.Rol;
import com.perubusSystem.perubus_system.model.Incidencia;
import com.perubusSystem.perubus_system.repository.IncidenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiereRol(Rol.AGENTE)
@RestController
@RequestMapping("/api/incidencias")
@CrossOrigin(origins = "${app.frontend-origin}", allowCredentials = "true")
public class IncidenciaController {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @GetMapping
    public List<Incidencia> listarIncidencias() {
        return incidenciaRepository.findAll();
    }

    @PostMapping
    public Incidencia reportarIncidencia(@RequestBody Incidencia incidencia) {
        return incidenciaRepository.save(incidencia);
    }
}