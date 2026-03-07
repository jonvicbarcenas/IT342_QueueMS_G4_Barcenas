package edu.cit.barcenas.queuems.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.FirebaseToken;
import edu.cit.barcenas.queuems.dto.LoginRequestDTO;
import edu.cit.barcenas.queuems.dto.RegisterRequestDTO;
import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final WebClient webClient;

    @Value("${firebase.api.key}")
    private String firebaseApiKey;

    public AuthService(UserRepository userRepository, JwtService jwtService, WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.webClient = webClientBuilder.baseUrl("https://identitytoolkit.googleapis.com").build();
    }

    public void register(RegisterRequestDTO dto) throws Exception {
        CreateRequest request = new CreateRequest()
                .setEmail(dto.getEmail())
                .setEmailVerified(false)
                .setPassword(dto.getPassword())
                .setDisabled(false);

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        User user = new User(userRecord.getUid(), dto.getEmail(), dto.getFirstname(), dto.getLastname(), "USER");
        userRepository.save(user);
    }

    /**
     * Login with email/password via Firebase REST API (signInWithPassword).
     * On success verify the returned ID token with Admin SDK and issue backend JWT.
     */
    public String login(LoginRequestDTO dto) throws Exception {
        // call Firebase REST API signInWithPassword
        var resp = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/accounts:signInWithPassword")
                        .queryParam("key", firebaseApiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SignInPayload(dto.getEmail(), dto.getPassword(), true))
                .retrieve()
                .bodyToMono(FirebaseSignInResponse.class)
                .block();

        if (resp == null || resp.idToken == null) {
            throw new RuntimeException("Failed to sign in");
        }

        // verify ID token with Admin SDK
        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(resp.idToken);
        String uid = decoded.getUid();

        // get user doc
        User user = userRepository.findByUid(uid);
        String role = user != null ? user.getRole() : "USER";

        // create backend token
        return jwtService.generateToken(uid, dto.getEmail(), role);
    }

    // local helper classes
    private static record SignInPayload(String email, String password, boolean returnSecureToken) {
    }

    private static class FirebaseSignInResponse {
        public String idToken;
        public String refreshToken;
        public String localId;
        public String email;
        public long expiresIn;
    }
}
