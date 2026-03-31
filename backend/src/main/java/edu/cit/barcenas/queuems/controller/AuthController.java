package edu.cit.barcenas.queuems.controller;

import edu.cit.barcenas.queuems.dto.AuthResponseDTO;
import edu.cit.barcenas.queuems.dto.LoginRequestDTO;
import edu.cit.barcenas.queuems.dto.RegisterRequestDTO;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
        // This endpoint returns the OAuth2 login URL for the frontend to redirect to
        String googleLoginUrl = "/oauth2/authorization/google";
        return ResponseEntity.ok(Map.of("redirectUrl", googleLoginUrl));
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
}
