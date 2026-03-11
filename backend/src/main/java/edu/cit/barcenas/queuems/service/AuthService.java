package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.dto.LoginRequestDTO;
import edu.cit.barcenas.queuems.dto.RegisterRequestDTO;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequestDTO dto) throws Exception {
        // Check if user already exists
        User existingUser = userRepository.findByEmail(dto.getEmail());
        if (existingUser != null) {
            throw new RuntimeException("User with this email already exists");
        }

        // Generate unique user ID
        String uid = UUID.randomUUID().toString();
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        
        // Create and save user
        User user = new User(uid, dto.getEmail(), hashedPassword, dto.getFirstname(), dto.getLastname(), "USER");
        userRepository.save(user);
    }

    /**
     * Login with email/password using custom authentication.
     * Verifies password and issues backend JWT.
     */
    public String login(LoginRequestDTO dto) throws Exception {
        // Find user by email
        User user = userRepository.findByEmail(dto.getEmail());
        if (user == null) {
            throw new RuntimeException("Invalid email or password");
        }

        // Verify password
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate JWT token
        return jwtService.generateToken(user.getUid(), user.getEmail(), user.getRole());
    }
}
