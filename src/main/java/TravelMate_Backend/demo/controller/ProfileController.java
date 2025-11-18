package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.UserProfileResponse;
import TravelMate_Backend.demo.dto.UserUpdate;
import TravelMate_Backend.demo.model.User;
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
}
