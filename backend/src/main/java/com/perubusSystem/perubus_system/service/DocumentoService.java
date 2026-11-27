package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.dto.DocumentoRespuesta;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.util.NombrePersona;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.NoSuchElementException;

// Consulta de DNI (8 dígitos) y RUC (11 dígitos): primero la BD local, luego la API de Decolecta.
// El token solo vive en el backend; el navegador nunca lo ve.
@Service
public class DocumentoService {

    private final ClienteRepository clienteRepository;
    private final RestClient restClient;
    private final String token;

    @Autowired
    public DocumentoService(ClienteRepository clienteRepository,
            @Value("${decolecta.url}") String baseUrl,
            @Value("${decolecta.token:}") String token) {
        this(clienteRepository, RestClient.builder().baseUrl(baseUrl).build(), token);
    }

    // Usado también por los tests con una API simulada
    DocumentoService(ClienteRepository clienteRepository, RestClient restClient, String token) {
        this.clienteRepository = clienteRepository;
        this.restClient = restClient;
        this.token = token;
    }

    public DocumentoRespuesta consultar(String numero) {
        String doc = numero == null ? "" : numero.trim();
        if (!doc.matches("\\d{8}|\\d{11}"))
            throw new IllegalArgumentException("El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC).");
        String tipo = doc.length() == 8 ? "DNI" : "RUC";

        var local = clienteRepository.findByNumDocumento(doc);
        if (local.isPresent()) {
            Cliente c = local.get();
            String nombres = c.getNombres();
            String apellidos = c.getApellidos() == null ? "" : c.getApellidos();
            if ("DNI".equals(tipo)) {
                nombres = NombrePersona.formatoTitulo(nombres);
                apellidos = NombrePersona.formatoTitulo(apellidos);
            }
            return new DocumentoRespuesta(tipo, doc, nombres, apellidos, "LOCAL");
        }

        if (token == null || token.isBlank())
            throw new IllegalStateException("La consulta externa no está configurada (falta DECOLECTA_TOKEN).");

        return "DNI".equals(tipo) ? consultarDni(doc) : consultarRuc(doc);
    }

    @SuppressWarnings("unchecked")
    private DocumentoRespuesta consultarDni(String dni) {
        Map<String, Object> cuerpo = llamar("/v1/reniec/dni", dni);
        String apellidos = (texto(cuerpo, "first_last_name") + " " + texto(cuerpo, "second_last_name")).trim();
        return new DocumentoRespuesta("DNI", dni,
                NombrePersona.formatoTitulo(texto(cuerpo, "first_name")),
                NombrePersona.formatoTitulo(apellidos), "API");
    }

    private DocumentoRespuesta consultarRuc(String ruc) {
        Map<String, Object> cuerpo = llamar("/v1/sunat/ruc", ruc);
        return new DocumentoRespuesta("RUC", ruc, texto(cuerpo, "razon_social"), "", "API");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> llamar(String ruta, String numero) {
        try {
            Map<String, Object> cuerpo = restClient.get()
                    .uri(ruta + "?numero={numero}", numero)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (cuerpo == null)
                throw new NoSuchElementException("No se encontró información para el documento " + numero + ".");
            return cuerpo;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 400 || status == 404 || status == 422)
                throw new NoSuchElementException("No se encontró información para el documento " + numero + ".");
            if (status == 401 || status == 403)
                throw new IllegalStateException("El token de la API externa es inválido o no tiene permiso.");
            if (status == 429)
                throw new IllegalStateException("Se excedió el límite de consultas de la API externa.");
            throw new IllegalStateException("La API externa respondió con error (" + status + ").");
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("No se pudo conectar con la API externa.");
        }
    }

    private String texto(Map<String, Object> cuerpo, String campo) {
        Object valor = cuerpo.get(campo);
        return valor == null ? "" : valor.toString().trim();
    }
}
