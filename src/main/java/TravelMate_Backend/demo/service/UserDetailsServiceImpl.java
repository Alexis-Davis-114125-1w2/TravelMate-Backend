package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.UserProfileResponse;
import TravelMate_Backend.demo.dto.UserUpdate;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
        
        return user;
    }


    public UserProfileResponse updateProfile(UserUpdate userUpdate, User currentUser) {
        try {
            // Verificar si el email ya existe (excepto el del usuario actual)
            if (!currentUser.getEmail().equals(userUpdate.getEmail())) {
                if (userRepository.existsByEmail(userUpdate.getEmail())) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "El email ya está en uso por otro usuario");
                    throw new RuntimeException(error.toString());
                }
            }
            User realUser = userRepository.getById(currentUser.getId());

            // Actualizar datos
            realUser.setName(userUpdate.getName());
            realUser.setEmail(userUpdate.getEmail());

            // Solo actualizar la imagen si se proporciona
            if (userUpdate.getProfilePictureUrl() != null &&
                    !userUpdate.getProfilePictureUrl().trim().isEmpty()) {
                realUser.setProfilePictureUrl(userUpdate.getProfilePictureUrl());
            }

            // Guardar cambios
            User updatedUser = userRepository.save(realUser);

            // Crear respuesta
            UserProfileResponse response = new UserProfileResponse(
                    updatedUser.getId(),
                    updatedUser.getName(),
                    updatedUser.getEmail(),
                    updatedUser.getProfilePictureUrl(),
                    updatedUser.getProvider().toString(),
                    updatedUser.getEmailVerified(),
                    updatedUser.getCreatedAt(),
                    updatedUser.getUpdatedAt()
            );

            return response;

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al actualizar el perfil: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,error.toString());
        }
    }

    public void deleteProfilePicture(User currentUser) {
        try {
            currentUser.setProfilePictureUrl(null);
            userRepository.save(currentUser);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Foto de perfil eliminada exitosamente");

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al eliminar la foto: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,error.toString());
        }
    }

    public String getPhoto(User currentUser) {
        User realUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return realUser.getProfilePictureUrl();
    }

    public String getPhotoByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getProfilePictureUrl();
    }
}
