package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.model.AuthProvider;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        try {
            // Procesar el usuario y guardarlo en la base de datos
            processOAuth2User(oAuth2User);
            // Devolver el OAuth2User original
            return oAuth2User;
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException("Error procesando usuario OAuth2");
        }
    }
    
    public User processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String profilePictureUrl = oAuth2User.getAttribute("picture");
        
        if (email == null) {
            throw new RuntimeException("Email no puede ser null");
        }
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider() == AuthProvider.LOCAL) {
                user.setGoogleId(googleId);
                user.setProvider(AuthProvider.GOOGLE);
                user.setProfilePictureUrl(profilePictureUrl);
                user = userRepository.save(user);
            }
        } else {
            user = new User();
            user.setName(name != null ? name : email.split("@")[0]);
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setProfilePictureUrl(profilePictureUrl);
            user.setProvider(AuthProvider.GOOGLE);
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }
        
        return user;
    }
}
