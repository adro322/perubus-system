package com.perubusSystem.perubus_system.service;

import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// La API de Decolecta se simula con MockRestServiceServer: no se gastan consultas reales
class DocumentoServiceTest {

    private static final String BASE = "http://api.test";

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private MockRestServiceServer api;
    private DocumentoService service;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        api = MockRestServiceServer.bindTo(builder).build();
        service = new DocumentoService(clienteRepository, builder.build(), "token-prueba");
    }

    @Test
    void rechazaFormatoInvalido() {
        assertThatThrownBy(() -> service.consultar("1234"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultar("12345678A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usaLaBaseLocalSinLlamarALaApi() {
        Cliente c = new Cliente();
        c.setNumDocumento("12345678");
        c.setNombres("Ana");
        c.setApellidos("Perez");
        when(clienteRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(c));

        var r = service.consultar("12345678");

        assertThat(r.origen()).isEqualTo("LOCAL");
        assertThat(r.nombres()).isEqualTo("Ana");
        api.verify(); // sin expectativas: falla si se hubiera llamado a la API
    }

    @Test
    void consultaDniEnLaApiYEnviaElToken() {
        when(clienteRepository.findByNumDocumento("46027897")).thenReturn(Optional.empty());
        api.expect(requestTo(BASE + "/v1/reniec/dni?numero=46027897"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-prueba"))
                .andRespond(withSuccess("""
                        {"first_name":"ROXANA KARINA","first_last_name":"DELGADO",
                         "second_last_name":"HUAMANI","document_number":"46027897"}""",
                        MediaType.APPLICATION_JSON));

        var r = service.consultar("46027897");

        assertThat(r.tipo()).isEqualTo("DNI");
        assertThat(r.nombres()).isEqualTo("ROXANA KARINA");
        assertThat(r.apellidos()).isEqualTo("DELGADO HUAMANI");
        assertThat(r.origen()).isEqualTo("API");
        api.verify();
    }

    @Test
    void consultaRucYDevuelveRazonSocial() {
        when(clienteRepository.findByNumDocumento("20601030013")).thenReturn(Optional.empty());
        api.expect(requestTo(BASE + "/v1/sunat/ruc?numero=20601030013"))
                .andRespond(withSuccess("{\"razon_social\":\"EMPRESA SAC\",\"estado\":\"ACTIVO\"}",
                        MediaType.APPLICATION_JSON));

        var r = service.consultar("20601030013");

        assertThat(r.tipo()).isEqualTo("RUC");
        assertThat(r.nombres()).isEqualTo("EMPRESA SAC");
        assertThat(r.apellidos()).isEmpty();
    }

    @Test
    void documentoInexistenteEsNoEncontrado() {
        when(clienteRepository.findByNumDocumento("00000000")).thenReturn(Optional.empty());
        api.expect(requestTo(BASE + "/v1/reniec/dni?numero=00000000"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.consultar("00000000"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void limiteExcedidoYTokenInvalidoSeReportanComoServicioNoDisponible() {
        when(clienteRepository.findByNumDocumento("11111111")).thenReturn(Optional.empty());
        api.expect(requestTo(BASE + "/v1/reniec/dni?numero=11111111"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> service.consultar("11111111"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("límite");

        api.reset();
        api.expect(requestTo(BASE + "/v1/reniec/dni?numero=11111111"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.consultar("11111111"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token");
    }

    @Test
    void sinTokenConfiguradoFallaSinLlamarALaApi() {
        var sinToken = new DocumentoService(clienteRepository, RestClient.builder().baseUrl(BASE).build(), "");
        when(clienteRepository.findByNumDocumento("22222222")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sinToken.consultar("22222222"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DECOLECTA_TOKEN");
    }
}
