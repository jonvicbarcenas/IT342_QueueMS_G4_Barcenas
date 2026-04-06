package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.dto.LoginRequestDTO;
import edu.cit.barcenas.queuems.dto.RegisterRequestDTO;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.pattern.adapter.UserAdapter;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Authentication Service implementing Singleton pattern via Spring @Service annotation.
 * This service handles user registration, login, and profile retrieval.
 * Act as a Facade for the auth subsystem.
 */
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

    /**
     * Process OAuth2 login/registration and return a backend JWT.
     */
    public String handleOAuth2Login(OAuth2User oAuth2User) throws Exception {
        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            // Create new user using Adapter pattern
            user = UserAdapter.adapt(oAuth2User);
            user.setUid(UUID.randomUUID().toString());
            userRepository.save(user);
        }
        
        // Generate JWT token
        return jwtService.generateToken(user.getUid(), user.getEmail(), user.getRole());
    }

    /**
     * Retrieve the authenticated user's profile information by their UID
     * @param uid the user's unique identifier
     * @return User object containing user information, or null if not found
     * @throws ExecutionException if the Firestore operation fails
     * @throws InterruptedException if the Firestore operation is interrupted
     */
    public User getUserById(String uid) throws ExecutionException, InterruptedException {
        return userRepository.findByUid(uid);
    }
}
