package edu.cit.barcenas.queuems.config;

import edu.cit.barcenas.queuems.model.Role;
import edu.cit.barcenas.queuems.service.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final boolean googleOAuthEnabled;

    public SecurityConfig(
            OAuth2SuccessHandler oAuth2SuccessHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.oauth2.google.enabled:false}") boolean googleOAuthEnabled) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleOAuthEnabled = googleOAuthEnabled;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/auth/**", "/login/oauth2/**", "/oauth2/**").permitAll()
                        // Teller endpoints - require TELLER or SUPERADMIN role
                        .requestMatchers("/api/teller/**").hasAnyRole(Role.TELLER, Role.SUPERADMIN)
                        // Admin endpoints - require SUPERADMIN role
                        .requestMatchers("/api/admin/**").hasRole(Role.SUPERADMIN)
                        // User endpoints - require authentication
                        .requestMatchers("/api/requests/**").authenticated()
                        .requestMatchers("/api/counters/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated());

        if (googleOAuthEnabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2SuccessHandler)
            );
        }

        // Add JWT Authentication Filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.asList("http://localhost:5173", "http://localhost:5174", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
