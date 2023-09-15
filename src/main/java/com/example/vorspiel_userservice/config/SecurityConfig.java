package com.example.vorspiel_userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Configuration class to authentiacate requests.<p>
 * 
 * @since 0.0.1
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Value("${FRONTEND_BASE_URL}")
    private String frontendBaseUrl;

    @Value("${CSRF_ENABLED}")
    private String csrfEnabled;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // enable csrf in prod only
        if (csrfEnabled.equalsIgnoreCase("true"))
            http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        else
            http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(request -> request
            .anyRequest()
                .permitAll());

        return http.build();
    }

    
    /**
     * Allow frontend url with any pattern.
     * 
     * @return
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(frontendBaseUrl)
                        .allowedMethods("GET", "POST", "UPDATE", "DELETE");
            }
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(10);
    }
}