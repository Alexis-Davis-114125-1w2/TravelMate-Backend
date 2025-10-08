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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("=== OAuth2UserService.loadUser INICIADO ===");
        System.out.println("Client Registration ID: " + userRequest.getClientRegistration().getRegistrationId());
        
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("OAuth2User obtenido de Google: " + oAuth2User.getName());
        
        try {
            // Procesar el usuario y guardarlo en la base de datos
            User savedUser = processOAuth2User(oAuth2User);
            System.out.println("Usuario procesado y guardado: " + savedUser.getId());
            // Devolver el OAuth2User original
            return oAuth2User;
        } catch (Exception ex) {
            System.err.println("Error procesando usuario OAuth2: " + ex.getMessage());
            ex.printStackTrace();
            throw new OAuth2AuthenticationException("Error procesando usuario OAuth2: " + ex.getMessage());
        }
    }
    
    @Transactional
    public User processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String profilePictureUrl = oAuth2User.getAttribute("picture");
        
        System.out.println("=== OAuth2UserService.processOAuth2User ===");
        System.out.println("Email: " + email);
        System.out.println("Name: " + name);
        System.out.println("Google ID: " + googleId);
        System.out.println("Profile Picture: " + profilePictureUrl);
        
        if (email == null) {
            throw new RuntimeException("Email no puede ser null");
        }
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        System.out.println("Usuario existente: " + userOptional.isPresent());
        
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
            System.out.println("Creando nuevo usuario OAuth2...");
            user = new User();
            user.setName(name != null ? name : email.split("@")[0]);
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setProfilePictureUrl(profilePictureUrl);
            user.setProvider(AuthProvider.GOOGLE);
            user.setEmailVerified(true);
            // Para usuarios OAuth2, establecer una contraseña temporal
            user.setPassword("OAUTH2_USER_PASSWORD");
            
            System.out.println("Guardando usuario en la base de datos...");
            try {
                user = userRepository.save(user);
                System.out.println("Usuario guardado con ID: " + user.getId());
                
                // Verificar que el usuario se guardó correctamente
                Optional<User> savedUserCheck = userRepository.findByEmail(email);
                if (savedUserCheck.isPresent()) {
                    System.out.println("Verificación: Usuario encontrado en BD después de guardar");
                } else {
                    System.err.println("ERROR: Usuario no encontrado en BD después de guardar");
                }
            } catch (Exception e) {
                System.err.println("ERROR al guardar usuario: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        
        return user;
    }
}
