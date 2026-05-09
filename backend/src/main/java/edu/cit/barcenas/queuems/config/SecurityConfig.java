package edu.cit.barcenas.queuems.config;

import edu.cit.barcenas.queuems.model.Role;
import edu.cit.barcenas.queuems.feature.auth.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String googleOAuthEnabled;
    private final String googleClientId;
    private final String googleClientSecret;

    public SecurityConfig(
            OAuth2SuccessHandler oAuth2SuccessHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.oauth2.google.enabled:auto}") String googleOAuthEnabled,
            @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleOAuthEnabled = googleOAuthEnabled;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
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
                        .requestMatchers("/api/auth/**", "/login/oauth2/**", "/oauth2/**", "/ws/**", "/ws-native/**").permitAll()
                        // Teller endpoints - require TELLER or SUPERADMIN role
                        .requestMatchers("/api/teller/**").hasAnyRole(Role.TELLER, Role.SUPERADMIN)
                        // Admin endpoints - require SUPERADMIN role
                        .requestMatchers("/api/admin/**").hasRole(Role.SUPERADMIN)
                        // User endpoints - require authentication
                        .requestMatchers("/api/requests/**").authenticated()
                        .requestMatchers("/api/counters/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated());

        if (isGoogleOAuthEnabled()) {
            http.oauth2Login(oauth2 -> oauth2
                    .clientRegistrationRepository(googleClientRegistrationRepository())
                    .successHandler(oAuth2SuccessHandler)
            );
        }

        // Add JWT Authentication Filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isGoogleOAuthEnabled() {
        if ("true".equalsIgnoreCase(googleOAuthEnabled)) {
            return true;
        }

        if ("false".equalsIgnoreCase(googleOAuthEnabled)) {
            return false;
        }

        return hasGoogleOAuthCredentials();
    }

    private boolean hasGoogleOAuthCredentials() {
        return StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret);
    }

    private ClientRegistrationRepository googleClientRegistrationRepository() {
        if (!hasGoogleOAuthCredentials()) {
            throw new IllegalStateException(
                    "Google OAuth is enabled, but GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET is missing.");
        }

        return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(googleClientId)
                        .clientSecret(googleClientSecret)
                        .scope("openid", "profile", "email")
                        .build());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.asList(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:3000",
                        "http://10.0.2.2:8080",
                        "http://192.168.1.6:8080"
                ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
