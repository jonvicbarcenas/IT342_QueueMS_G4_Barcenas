package edu.cit.barcenas.queuems.service;

import edu.cit.barcenas.queuems.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        try {
            // Use AuthService as a Facade to handle the login/registration logic
            String token = authService.handleOAuth2Login(oAuth2User);
            
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
