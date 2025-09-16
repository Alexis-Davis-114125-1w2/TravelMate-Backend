package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.AuthProvider;
import TravelMate_Backend.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByGoogleId(String googleId);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByGoogleId(String googleId);
    
    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);
}
