package com.payroll.config;

import com.payroll.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
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
            .authorizeHttpRequests(auth -> auth
                // Static assets and page entry points
                .requestMatchers(
                    "/", "/index.html", "/app.html",
                    "/css/**", "/js/**", "/images/**", "/favicon.ico"
                ).permitAll()
                // Authentication API
                .requestMatchers("/api/auth/**").permitAll()
                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Employee self-service salary structure
                .requestMatchers(HttpMethod.GET, "/api/salaries/me").authenticated()
                // Admin and HR endpoints
                .requestMatchers("/api/employees/**", "/api/departments/**", "/api/salaries/**").hasAnyRole("ADMIN", "HR")
                // Payroll processing & records (Employee specific PDF download allowed via service ownership check)
                .requestMatchers(HttpMethod.GET, "/api/payroll/{id}/payslip/pdf").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/payroll/{id}/payslip").authenticated()
                .requestMatchers("/api/payroll/**").hasAnyRole("ADMIN", "HR")
                // Manager endpoints
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN", "HR")
                // Reports endpoints
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                // Common authenticated self-service endpoints (Employee, Profile, Notifications)
                .requestMatchers("/api/employee/me/**", "/api/profile/**", "/api/notifications/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized. Please log in.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"success\":false,\"message\":\"Access Denied: Insufficient permissions.\"}");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("{\"success\":true,\"message\":\"Logged out successfully\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        http.authenticationProvider(authenticationProvider());
        return http.build();
    }
}
