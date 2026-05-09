package edu.cit.barcenas.queuems.feature.auth;

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
            
            // Determine redirect URL
            String redirectUrl;
            String userAgent = request.getHeader("User-Agent");
            boolean isMobile = userAgent != null && (userAgent.contains("Android") || userAgent.contains("iPhone"));

            if (isMobile) {
                // Redirect back to the mobile app via deep link
                redirectUrl = String.format("queuems://auth/callback?token=%s", token);
            } else {
                // Redirect to frontend with token
                redirectUrl = String.format("%s/auth/callback?token=%s", frontendUrl, token);
            }

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            
        } catch (Exception e) {
            String errorMessage = e.getMessage().replace(" ", "+");
            String redirectUrl;
            String userAgent = request.getHeader("User-Agent");
            boolean isMobile = userAgent != null && (userAgent.contains("Android") || userAgent.contains("iPhone"));

            if (isMobile) {
                redirectUrl = String.format("queuems://auth/callback?error=%s", errorMessage);
            } else {
                redirectUrl = String.format("%s/auth/callback?error=%s", frontendUrl, errorMessage);
            }
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}
