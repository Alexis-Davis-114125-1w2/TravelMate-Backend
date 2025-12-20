package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.*;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.service.EmailChangeService;
import TravelMate_Backend.demo.service.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController()
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private EmailChangeService emailChangeService;

    /**
     * Actualizar perfil del usuario
     */
    @PutMapping()
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UserUpdate userUpdate,
            @AuthenticationPrincipal User currentUser) {
        UserProfileResponse userProfileResponse = userDetailsService.updateProfile(userUpdate,currentUser);
        return ResponseEntity.ok(userProfileResponse);
    }

    /**
     * Eliminar foto de perfil
     */
    @DeleteMapping("/picture")
    public ResponseEntity<?> deleteProfilePicture(
            @AuthenticationPrincipal User currentUser) {
        userDetailsService.deleteProfilePicture(currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/photo")
    public ResponseEntity<?> getProfilePicture(
            @AuthenticationPrincipal User currentUser){
        String photoProfile = userDetailsService.getPhoto(currentUser);
        return ResponseEntity.ok(photoProfile);
    }

    @GetMapping("/{userId}/photo")
    public ResponseEntity<?> getUserProfilePicture(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser){
        try {
            String photoProfile = userDetailsService.getPhotoByUserId(userId);
            return ResponseEntity.ok(photoProfile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Photo not found for user: " + userId);
        }
    }

    /**
     * Iniciar cambio de email - envía código al email actual
     */
    @PostMapping("/email/change/initiate")
    public ResponseEntity<?> initiateEmailChange(
            @Valid @RequestBody EmailChangeRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            emailChangeService.sendEmailChangeCode(currentUser.getEmail(), request.getNewEmail());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Se ha enviado un código de verificación a tu email actual");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verificar código de cambio de email
     */
    @PostMapping("/email/change/verify")
    public ResponseEntity<?> verifyEmailChangeCode(
            @Valid @RequestBody VerifyEmailChangeCodeRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            boolean isValid = emailChangeService.verifyCode(currentUser.getEmail(), request.getCode());
            if (isValid) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Código verificado correctamente");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Código inválido");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Confirmar cambio de email
     */
    @PostMapping("/email/change/confirm")
    public ResponseEntity<?> confirmEmailChange(
            @Valid @RequestBody ConfirmEmailChangeRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            emailChangeService.confirmEmailChange(currentUser.getEmail(), request.getCode());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email actualizado exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Eliminar cuenta permanentemente
     */
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal User currentUser) {
        try {
            userDetailsService.deleteAccount(currentUser);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cuenta eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
