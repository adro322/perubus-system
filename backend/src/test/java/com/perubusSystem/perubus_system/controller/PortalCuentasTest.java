package com.perubusSystem.perubus_system.controller;

import com.perubusSystem.perubus_system.dto.PortalCambioClaveSolicitud;
import com.perubusSystem.perubus_system.dto.PortalLoginSolicitud;
import com.perubusSystem.perubus_system.dto.PortalRegistroSolicitud;
import com.perubusSystem.perubus_system.model.Cliente;
import com.perubusSystem.perubus_system.repository.BultoEncomiendaRepository;
import com.perubusSystem.perubus_system.repository.ClienteRepository;
import com.perubusSystem.perubus_system.repository.EncomiendaRepository;
import com.perubusSystem.perubus_system.repository.EventoTrackingRepository;
import com.perubusSystem.perubus_system.service.EstimacionEntregaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cuentas del portal: activación verificada, clave temporal, bloqueo y límite de intentos
class PortalCuentasTest {

    private static final String DOC = "45218763";

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final PortalClienteController controller = new PortalClienteController(
            clienteRepository,
            mock(BultoEncomiendaRepository.class),
            mock(EncomiendaRepository.class),
            mock(EventoTrackingRepository.class),
            new EstimacionEntregaService(new StandardEnvironment()));
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Cliente cliente = new Cliente();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void preparar() {
        cliente.setIdCliente(3L);
        cliente.setNumDocumento(DOC);
        cliente.setNombres("Carlos Alberto");
        cliente.setApellidos("Mendoza Quispe");
        cliente.setCorreo("carlos.mendoza@gmail.com");
        cliente.setTelefono("987412365");
        when(clienteRepository.findByNumDocumento(DOC)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PortalRegistroSolicitud registro(String doc, String correo, String telefono) {
        return new PortalRegistroSolicitud(doc, "Nombre Falso", "Apellido Falso", telefono, correo, "Clave-segura-1");
    }

    private String mensaje(org.springframework.http.ResponseEntity<?> respuesta) {
        return String.valueOf(((Map<?, ?>) respuesta.getBody()).get("mensaje"));
    }

    // ---------- Activación de la cuenta ----------

    @Test
    void noCreaClientesNuevosDesdeElPortal() {
        when(clienteRepository.findByNumDocumento("70000000")).thenReturn(Optional.empty());

        var r = controller.registrar(registro("70000000", "falso@mail.com", "987654321"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void activaLaCuentaConCorreoYTelefonoRegistradosSinTocarLosNombres() {
        var r = controller.registrar(registro(DOC, "CARLOS.MENDOZA@gmail.com", "987412365"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(encoder.matches("Clave-segura-1", cliente.getPassword())).isTrue();
        assertThat(cliente.getNombres()).isEqualTo("Carlos Alberto");
        assertThat(cliente.debeCambiarClave()).isFalse();
    }

    @Test
    void documentoInexistenteYDatosIncorrectosRecibenElMismoMensaje() {
        when(clienteRepository.findByNumDocumento("70000000")).thenReturn(Optional.empty());

        var inexistente = controller.registrar(registro("70000000", "carlos.mendoza@gmail.com", "987412365"));
        var correoDistinto = controller.registrar(registro(DOC, "otra.persona@gmail.com", "987412365"));
        var telefonoDistinto = controller.registrar(registro(DOC, "carlos.mendoza@gmail.com", "900000000"));

        assertThat(mensaje(correoDistinto)).isEqualTo(mensaje(inexistente));
        assertThat(mensaje(telefonoDistinto)).isEqualTo(mensaje(inexistente));
        assertThat(cliente.tieneCuentaPortal()).isFalse();
    }

    @Test
    void sinCorreoRegistradoEnLaAgenciaNoSePuedeActivarSolo() {
        cliente.setCorreo(null);

        var r = controller.registrar(registro(DOC, "carlos.mendoza@gmail.com", "987412365"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cliente.tieneCuentaPortal()).isFalse();
    }

    @Test
    void noActivaUnaCuentaBloqueadaNiUnaQueYaExiste() {
        cliente.setPassword(encoder.encode("OtraClave123"));
        var yaExiste = controller.registrar(registro(DOC, "carlos.mendoza@gmail.com", "987412365"));
        assertThat(yaExiste.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(yaExiste)).contains("Ya existe una cuenta");

        cliente.setPortalBloqueado(true);
        var bloqueada = controller.registrar(registro(DOC, "carlos.mendoza@gmail.com", "987412365"));
        assertThat(bloqueada.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------- Clave temporal ----------

    private void cuentaConClaveTemporal(String temporal) {
        cliente.setPassword(encoder.encode(temporal));
        cliente.setDebeCambiarPassword(true);
    }

    @Test
    void conClaveTemporalSoloSePermiteCambiarLaClave() {
        cuentaConClaveTemporal("Temporal-123");

        var login = controller.iniciarSesion(new PortalLoginSolicitud(DOC, "Temporal-123"), request);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) login.getBody()).get("debeCambiarPassword")).isEqualTo(true);
        assertThat(request.getSession().getAttribute("portalClienteId")).isNull();
        assertThat(request.getSession().getAttribute("portalCambioPendienteId")).isEqualTo(3L);
        assertThat(controller.sesion(request.getSession()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.misEncomiendas(request.getSession()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cambiarLaClaveTemporalAbreLaSesionCompleta() {
        cuentaConClaveTemporal("Temporal-123");
        controller.iniciarSesion(new PortalLoginSolicitud(DOC, "Temporal-123"), request);

        var r = controller.cambiarPassword(new PortalCambioClaveSolicitud("Temporal-123", "MiClaveNueva-9"), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cliente.debeCambiarClave()).isFalse();
        assertThat(encoder.matches("MiClaveNueva-9", cliente.getPassword())).isTrue();
        assertThat(request.getSession().getAttribute("portalClienteId")).isEqualTo(3L);
        assertThat(request.getSession().getAttribute("portalCambioPendienteId")).isNull();
        assertThat(controller.sesion(request.getSession()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rechazaCambiosDeClaveInvalidos() {
        cuentaConClaveTemporal("Temporal-123");
        controller.iniciarSesion(new PortalLoginSolicitud(DOC, "Temporal-123"), request);

        var actualIncorrecta = controller.cambiarPassword(new PortalCambioClaveSolicitud("Equivocada-1", "MiClaveNueva-9"), request);
        var nuevaCorta = controller.cambiarPassword(new PortalCambioClaveSolicitud("Temporal-123", "corta"), request);
        var nuevaIgual = controller.cambiarPassword(new PortalCambioClaveSolicitud("Temporal-123", "Temporal-123"), request);

        assertThat(actualIncorrecta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(nuevaCorta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(nuevaIgual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cliente.debeCambiarClave()).isTrue();
    }

    @Test
    void cambiarLaClaveExigeIniciarSesion() {
        var r = controller.cambiarPassword(new PortalCambioClaveSolicitud("a", "MiClaveNueva-9"), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- Consulta de documento (datos enmascarados) ----------

    @Test
    void laConsultaDeDocumentoNuncaDevuelveLaFichaCompleta() {
        var r = controller.consultarDocumento(DOC, request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> cuerpo = (Map<?, ?>) r.getBody();
        assertThat(cuerpo.get("registrado")).isEqualTo(true);
        assertThat(cuerpo.get("nombre")).isEqualTo("Carlos M. Q.");
        assertThat(cuerpo.get("correo")).isEqualTo("c***@gmail.com");
        assertThat(cuerpo.get("telefono")).isEqualTo("*** *** 365");
        assertThat(cuerpo.get("tieneCuenta")).isEqualTo(false);
        assertThat(cuerpo.toString())
                .doesNotContain("Mendoza")
                .doesNotContain("carlos.mendoza")
                .doesNotContain("987412365");
    }

    @Test
    void laConsultaIndicaSiYaTieneCuentaOEstaBloqueada() {
        cliente.setPassword(encoder.encode("MiClave-123"));
        cliente.setPortalBloqueado(true);

        Map<?, ?> cuerpo = (Map<?, ?>) controller.consultarDocumento(DOC, request).getBody();

        assertThat(cuerpo.get("tieneCuenta")).isEqualTo(true);
        assertThat(cuerpo.get("bloqueada")).isEqualTo(true);
    }

    @Test
    void documentoNoRegistradoSoloDiceQueNoEstaRegistrado() {
        when(clienteRepository.findByNumDocumento("70000002")).thenReturn(Optional.empty());

        Map<?, ?> cuerpo = (Map<?, ?>) controller.consultarDocumento("70000002", request).getBody();

        assertThat(cuerpo).hasSize(1);
        assertThat(cuerpo.get("registrado")).isEqualTo(false);
    }

    @Test
    void laConsultaRechazaDocumentosMalFormados() {
        assertThat(controller.consultarDocumento("123", request).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.consultarDocumento("abcdefgh", request).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void laConsultaDeDocumentosTieneLimitePorDireccionIp() {
        for (int i = 0; i < 30; i++) {
            assertThat(controller.consultarDocumento(DOC, request).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        assertThat(controller.consultarDocumento(DOC, request).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void empresaMuestraSuRazonSocialYClientesSinDatosNoFallan() {
        Cliente empresa = new Cliente();
        empresa.setNombres("Distribuidora Ica Norte S.A.C.");
        empresa.setApellidos("");
        assertThat(PortalClienteController.nombreParcial(empresa)).isEqualTo("Distribuidora Ica Norte S.A.C.");
        assertThat(PortalClienteController.correoParcial(null)).isEmpty();
        assertThat(PortalClienteController.correoParcial("a@b.pe")).isEqualTo("a***@b.pe");
        assertThat(PortalClienteController.telefonoParcial(null)).isEmpty();
    }

    // ---------- Cuenta bloqueada y límite de intentos ----------

    @Test
    void cuentaBloqueadaSoloLoDiceConLaClaveCorrecta() {
        cliente.setPassword(encoder.encode("MiClave-123"));
        cliente.setPortalBloqueado(true);

        var claveMala = controller.iniciarSesion(new PortalLoginSolicitud(DOC, "Incorrecta-1"), request);
        var claveBuena = controller.iniciarSesion(new PortalLoginSolicitud(DOC, "MiClave-123"), request);

        assertThat(claveMala.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(claveBuena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(request.getSession().getAttribute("portalClienteId")).isNull();
    }

    @Test
    void tras5FallosElLoginQuedaLimitadoAunConLaClaveCorrecta() {
        cliente.setPassword(encoder.encode("MiClave-123"));

        for (int i = 0; i < 5; i++) {
            var r = controller.iniciarSesion(new PortalLoginSolicitud(DOC, "Incorrecta-" + i), request);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        var bloqueado = controller.iniciarSesion(new PortalLoginSolicitud(DOC, "MiClave-123"), request);

        assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void unBloqueoEnMedioDeLaSesionLeQuitaElAccesoAMisEncomiendas() {
        cliente.setPassword(encoder.encode("MiClave-123"));
        controller.iniciarSesion(new PortalLoginSolicitud(DOC, "MiClave-123"), request);
        assertThat(controller.sesion(request.getSession()).getStatusCode()).isEqualTo(HttpStatus.OK);

        cliente.setPortalBloqueado(true);

        assertThat(controller.sesion(request.getSession()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
