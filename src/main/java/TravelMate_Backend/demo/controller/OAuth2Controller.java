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
            User user = authService.findByEmail(email).orElse(null);
            
            if (user == null) {
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
            User user = authService.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Usuario no encontrado"));
            }
            
            String token = authService.generateTokenForUser(user);
            
            // Redirigir al frontend con el token
            String redirectUrl = "http://localhost:3000/auth/callback?token=" + token;
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error en autenticación OAuth2: " + e.getMessage()));
        }
    }
}
