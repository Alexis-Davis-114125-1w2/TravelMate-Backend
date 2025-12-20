package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {

    Optional<EmailChangeToken> findByEmailAndCodeAndUsedFalse(String email, String code);

    void deleteByEmail(String email);
}

