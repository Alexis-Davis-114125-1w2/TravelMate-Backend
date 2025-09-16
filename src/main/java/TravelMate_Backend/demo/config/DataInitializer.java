package TravelMate_Backend.demo.config;

import TravelMate_Backend.demo.model.AuthProvider;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Crear usuario de prueba si no existe
        if (!userRepository.existsByEmail("test@test.com")) {
            User testUser = new User();
            testUser.setName("Usuario de Prueba");
            testUser.setEmail("test@test.com");
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setProvider(AuthProvider.LOCAL);
            testUser.setEmailVerified(true);
            
            userRepository.save(testUser);
            System.out.println("Usuario de prueba creado: test@test.com / password123");
        }
        
        // Crear usuario admin si no existe
        if (!userRepository.existsByEmail("admin@travelmate.com")) {
            User adminUser = new User();
            adminUser.setName("Administrador");
            adminUser.setEmail("admin@travelmate.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setProvider(AuthProvider.LOCAL);
            adminUser.setEmailVerified(true);
            
            userRepository.save(adminUser);
            System.out.println("Usuario admin creado: admin@travelmate.com / admin123");
        }
    }
}
