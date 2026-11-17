package com.perubusSystem.perubus_system.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSeguridadConfig implements WebMvcConfigurer {

    @Autowired
    private AccesoPorRolInterceptor accesoPorRol;

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
    }
}
