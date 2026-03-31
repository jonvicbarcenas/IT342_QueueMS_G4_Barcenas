package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.model.User;
import edu.cit.barcenas.queuems.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        try {
            // Extract user information from OAuth2User
            String email = oAuth2User.getAttribute("email");
            String givenName = oAuth2User.getAttribute("given_name");
            String familyName = oAuth2User.getAttribute("family_name");
            
            // Check if user exists
            User user = userRepository.findByEmail(email);
            
            if (user == null) {
                // Create new user
                String uid = UUID.randomUUID().toString();
                user = new User(uid, email, null, givenName, familyName, "USER");
                userRepository.save(user);
            }
            
            // Generate JWT token
            String token = jwtService.generateToken(user.getUid(), user.getEmail(), user.getRole());
            
            // Redirect to frontend with token
            String redirectUrl = String.format("%s/auth/callback?token=%s", frontendUrl, token);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            
        } catch (Exception e) {
            // Redirect to frontend with error
            String redirectUrl = String.format("%s/auth/callback?error=%s", frontendUrl, 
                    e.getMessage().replace(" ", "+"));
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}
