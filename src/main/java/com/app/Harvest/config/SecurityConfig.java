package com.app.Harvest.config;

import com.app.Harvest.security.CustomUserDetailsService;
import com.app.Harvest.security.JwtAuthenticationEntryPoint;
import com.app.Harvest.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**", "/auth/**").permitAll()

                        // Cooperative public endpoints (for batch operations and microservices)
                        .requestMatchers("/api/cooperative/farmers/batch", "/cooperative/farmers/batch").permitAll()
                        .requestMatchers("/api/cooperative/farmers/by-location", "/cooperative/farmers/by-location").permitAll()
                        .requestMatchers("/api/cooperative/farmers/microservice/**", "/cooperative/farmers/microservice/**").permitAll()

                        // Super Admin only endpoints
                        .requestMatchers("/api/users/pending-approvals", "/users/pending-approvals").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/users/*/approve", "/users/*/approve").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/users/*/reject", "/users/*/reject").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/users/*/delete", "/users/*/delete").hasRole("SUPER_ADMIN")

                        // User management endpoints (Super Admin, Government, Admin)
                        .requestMatchers("/api/users/**", "/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "GOVERNMENT")

                        // Cooperative endpoints - accessible by COOPERATIVE role and admins
                        .requestMatchers("/api/cooperative/**", "/cooperative/**").hasAnyRole("SUPER_ADMIN", "COOPERATIVE", "GOVERNMENT")

                        // Farmer endpoints - accessible by COOPERATIVE role and admins
                        .requestMatchers("/api/farmers/**", "/farmers/**").hasAnyRole("SUPER_ADMIN", "COOPERATIVE", "GOVERNMENT")

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}