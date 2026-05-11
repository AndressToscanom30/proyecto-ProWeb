package com.cronos.gestiontributaria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro", "/403", "/css/**", "/js/**", "/images/**", "/error")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception.accessDeniedPage("/403"));

        return http.build();
    }
}