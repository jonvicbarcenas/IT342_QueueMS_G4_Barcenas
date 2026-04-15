package edu.cit.barcenas.queuems.pattern.adapter;

import edu.cit.barcenas.queuems.model.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class UserAdapter {
    public static User adapt(OAuth2User oauth2User) {
        User user = new User();
        user.setEmail(oauth2User.getAttribute("email"));
        user.setFirstname(oauth2User.getAttribute("given_name"));
        user.setLastname(oauth2User.getAttribute("family_name"));
        user.setRole("USER");
        return user;
    }
}
