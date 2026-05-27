package com.cronos.gestiontributaria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.cronos.gestiontributaria.auth.service.CustomUserDetailsService;

/**
 * Configuración de seguridad de la aplicación.
 *
 * <p>Define el codificador BCrypt, el proveedor de autenticación y las rutas
 * públicas, autenticadas y protegidas por rol.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        /**
         * Crea el codificador de contraseñas basado en BCrypt.
         *
         * @return encoder BCrypt con coste 12
         */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

        /**
         * Construye el proveedor de autenticación con el servicio de usuarios y el encoder.
         *
         * @param userDetailsService servicio que carga usuarios desde MongoDB
         * @param passwordEncoder encoder de contraseñas
         * @return proveedor de autenticación DAO
         */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

        /**
         * Define la cadena de filtros de Spring Security para la aplicación.
         *
         * @param http objeto de configuración HTTP
         * @param authenticationProvider proveedor de autenticación DAO
         * @return cadena de filtros construida
         * @throws Exception si ocurre un error al construir la configuración
         */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider)
            throws Exception {
        http.authenticationProvider(authenticationProvider)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/forgot-password", "/reset-password", "/403", "/css/**", "/js/**", "/images/**", "/error")
                        .permitAll()
                        // ── Área de GERENTE: bloquear CONTRIBUYENTE explícitamente ──
                        // Vistas MVC de gestión de clientes: visible para CONTADOR y AUXILIAR_CONTADOR también (pero en read-only).
                        .requestMatchers("/clientes/**")
                            .hasAnyRole("GERENTE", "ASESOR", "CONTADOR", "AUXILIAR_CONTADOR")
                        // ── API REST ──
                        // Listado REST de contribuyentes: GERENTE y ASESOR.
                        .requestMatchers(HttpMethod.GET, "/api/contribuyente")
                            .hasAnyRole("GERENTE", "ASESOR")
                        // toggle-active es para GERENTE aunque viva bajo /api/contribuyente.
                        .requestMatchers(HttpMethod.PATCH, "/api/contribuyente/*/toggle-active")
                            .hasRole("GERENTE")
                        // API de clientes (POST/PUT/DELETE): solo GERENTE.
                        .requestMatchers(HttpMethod.POST, "/api/clientes/**")
                            .hasRole("GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**")
                            .hasRole("GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**")
                            .hasRole("GERENTE")
                        .requestMatchers("/api/obligaciones/**")
                            .hasAnyRole("GERENTE", "ASESOR", "ADMIN", "CONTADOR", "AUXILIAR_CONTADOR")
                        // ── Vistas del Administrador / Empleados (Bloqueadas para CONTRIBUYENTE) ──
                        .requestMatchers("/dashboard", "/calendario-fiscal", "/tareas/**", "/reportes/**", "/empleados/**", "/configuracion/**", "/notificaciones/**", "/obligaciones/**", "/mis-obligaciones/**")
                            .hasAnyRole("GERENTE", "ASESOR", "ADMIN", "CONTADOR", "AUXILIAR_CONTADOR")
                        // Portal del contribuyente.
                        .requestMatchers("/api/contribuyente/**").hasRole("CONTRIBUYENTE")
                        .requestMatchers("/portal/**").hasRole("CONTRIBUYENTE")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // ── Mensajes internos: cualquier usuario autenticado ──
                        .requestMatchers("/api/mensajes/**").authenticated()
                        // ── Solicitudes: listado total solo ADMIN, resto autenticado ──
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes").hasRole("ADMIN")
                        .requestMatchers("/api/solicitudes/**").authenticated()
                        // Resto de la API REST queda como estaba antes: público.
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isContribuyente = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_CONTRIBUYENTE"));
                            if (isContribuyente) {
                                response.sendRedirect("/portal");
                            } else {
                                response.sendRedirect("/dashboard");
                            }
                        })
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?expired")
                        .maximumSessions(1)
                        .expiredUrl("/login?expired"))
                .exceptionHandling(exception -> exception.accessDeniedPage("/403"));

        return http.build();
    }
}