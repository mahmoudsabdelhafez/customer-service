package com.bank.customer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic authentication: a {@code VIEWER} role that may read, and an
 * {@code ADMIN} role that may also write. Swagger stays public.
 *
 * <p>Two in-memory users are enough to demonstrate the mechanics; swapping
 * {@link InMemoryUserDetailsManager} for an OAuth2 resource server would change
 * this class alone.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // No browser session, so no CSRF token to protect.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/**").hasAnyRole("VIEWER", "ADMIN")
                        .requestMatchers("/api/v1/customers/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.security.viewer-password}") String viewerPassword,
            @Value("${app.security.admin-password}") String adminPassword) {

        return new InMemoryUserDetailsManager(
                User.withUsername("viewer")
                        .password(passwordEncoder.encode(viewerPassword))
                        .roles("VIEWER")
                        .build(),
                User.withUsername("admin")
                        .password(passwordEncoder.encode(adminPassword))
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
