package com.perubusSystem.perubus_system.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSeguridadConfig implements WebMvcConfigurer {

    @Autowired
    private AccesoPorRolInterceptor accesoPorRol;

    @Autowired
    private PortalSesionInterceptor portalSesion;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accesoPorRol)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/portal/**",                // el portal del cliente usa su propia sesión
                        "/api/usuarios/login",
                        "/api/usuarios/salir",
                        "/api/usuarios/registrar",       // valida el permiso dentro del controlador
                        "/api/encomiendas/buscar/**");   // consulta pública de tracking

        // Sesión del cliente: se corta si su cuenta fue bloqueada
        registry.addInterceptor(portalSesion)
                .addPathPatterns("/api/portal/**")
                .excludePathPatterns("/api/portal/registro", "/api/portal/login", "/api/portal/salir");
    }
}
