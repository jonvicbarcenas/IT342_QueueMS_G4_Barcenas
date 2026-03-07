package edu.cit.barcenas.queuems.controller;

import edu.cit.barcenas.queuems.dto.AuthResponseDTO;
import edu.cit.barcenas.queuems.dto.LoginRequestDTO;
import edu.cit.barcenas.queuems.dto.RegisterRequestDTO;
import edu.cit.barcenas.queuems.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
