package com.bench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Boot application entry point for the Calculator REST API.
 */
@SpringBootApplication
@EnableWebSecurity
public class CalculatorApp {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CalculatorApp.class, args);
    }

    /**
     * Temporary SecurityFilterChain that permits all requests.
     * This allows the calculator endpoint to be accessible without authentication
     * until the auth controller is implemented in TASK-010.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}
