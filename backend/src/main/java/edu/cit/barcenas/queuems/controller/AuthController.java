package edu.cit.barcenas.queuems.controller;

import edu.cit.barcenas.queuems.dto.AuthResponseDTO;
import edu.cit.barcenas.queuems.dto.LoginRequestDTO;
import edu.cit.barcenas.queuems.dto.RegisterRequestDTO;
import edu.cit.barcenas.queuems.dto.UpdateProfileDTO;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;
    private final String googleOAuthEnabled;
    private final String googleClientId;
    private final String googleClientSecret;

    public AuthController(
            AuthService authService,
            @Value("${app.oauth2.google.enabled:auto}") String googleOAuthEnabled,
            @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret) {
        this.authService = authService;
        this.googleOAuthEnabled = googleOAuthEnabled;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO dto) {
        try {
            authService.register(dto);
            return ResponseEntity.status(201).body("User created");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        try {
            String backendToken = authService.login(dto);
            return ResponseEntity.ok(new AuthResponseDTO(backendToken, /* expires */ 86400000L));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/google/login")
    public ResponseEntity<?> googleLogin() {
        if (!isGoogleOAuthEnabled() || !hasGoogleOAuthCredentials()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message",
                            "Google login is not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET."));
        }

        String googleLoginUrl = "/oauth2/authorization/google";
        return ResponseEntity.ok(Map.of("redirectUrl", googleLoginUrl));
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

    /**
     * Get the authenticated user's profile information
     * Requires authentication - any authenticated user can access their own profile
     * @return User profile information
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String uid = (String) authentication.getPrincipal();
            
            User user = authService.getUserById(uid);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateMe(@RequestBody UpdateProfileDTO body) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String uid = (String) authentication.getPrincipal();
            return ResponseEntity.ok(authService.updateProfile(uid, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateFcmToken(@RequestBody Map<String, String> body) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String uid = (String) authentication.getPrincipal();
            String fcmToken = body.get("fcmToken");

            if (fcmToken == null) {
                return ResponseEntity.badRequest().body("fcmToken is required");
            }

            authService.updateFcmToken(uid, fcmToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
