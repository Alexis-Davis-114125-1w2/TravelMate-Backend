package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.AuthResponse;
import TravelMate_Backend.demo.model.AuthProvider;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.service.AuthService;
import TravelMate_Backend.demo.service.OAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {
    
    @Autowired
    private OAuth2UserService oAuth2UserService;
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/user")
    public ResponseEntity<?> getOAuth2User(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Usuario no autenticado"));
        }
        
        try {
            // El usuario ya fue procesado por OAuth2UserService
            String email = principal.getAttribute("email");
            System.out.println("Buscando usuario con email: " + email);
            
            User user = authService.findByEmail(email).orElse(null);
            
            if (user == null) {
                System.out.println("Usuario no encontrado en la base de datos");
                return ResponseEntity.badRequest().body(Map.of("message", "Usuario no encontrado"));
            }
            
            System.out.println("Usuario encontrado: " + user.getName() + " (" + user.getEmail() + ")");
            String token = authService.generateTokenForUser(user);
            
            AuthResponse response = new AuthResponse(
                    token,
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getProfilePictureUrl(),
                    user.getProvider().toString()
            );
            
            System.out.println("Token generado para usuario OAuth2");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error en getOAuth2User: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error procesando usuario OAuth2: " + e.getMessage()));
        }
    }
    
    @GetMapping("/success")
    public ResponseEntity<?> oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Autenticación OAuth2 fallida"));
        }
        
        try {
            // El usuario ya fue procesado por OAuth2UserService
            String email = principal.getAttribute("email");
            System.out.println("OAuth2 Success - Email: " + email);
            
            User user = authService.findByEmail(email).orElse(null);
            
            if (user == null) {
                System.out.println("Usuario no encontrado en OAuth2 Success");
                return ResponseEntity.badRequest().body(Map.of("message", "Usuario no encontrado"));
            }
            
            System.out.println("Usuario encontrado en OAuth2 Success: " + user.getName());
            String token = authService.generateTokenForUser(user);
            
            // Redirigir al frontend con el token
            String redirectUrl = "http://localhost:3000/auth/callback?token=" + token;
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
        } catch (Exception e) {
            System.err.println("Error en OAuth2 Success: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error en autenticación OAuth2: " + e.getMessage()));
        }
    }
    
    @GetMapping("/callback")
    public ResponseEntity<?> oauth2Callback(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Autenticación OAuth2 fallida"));
        }
        
        try {
            String email = principal.getAttribute("email");
            System.out.println("OAuth2 Callback - Email: " + email);
            
            User user = authService.findByEmail(email).orElse(null);
            
            if (user == null) {
                System.out.println("Usuario no encontrado en OAuth2 Callback");
                return ResponseEntity.badRequest().body(Map.of("message", "Usuario no encontrado"));
            }
            
            String token = authService.generateTokenForUser(user);
            
            AuthResponse response = new AuthResponse(
                    token,
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getProfilePictureUrl(),
                    user.getProvider().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error en OAuth2 Callback: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error en callback OAuth2: " + e.getMessage()));
        }
    }
}
