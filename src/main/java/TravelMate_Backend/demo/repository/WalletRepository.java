package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    // Buscar billetera general de un viaje
    Optional<Wallet> findByTripIdAndIsGeneralTrue(Long tripId);
    
    // Buscar billetera individual de un usuario en un viaje
    Optional<Wallet> findByTripIdAndUserIdAndIsGeneralFalse(Long tripId, Long userId);
    
    // Buscar todas las billeteras de un viaje
    List<Wallet> findByTripId(Long tripId);
    
    // Buscar todas las billeteras individuales de un usuario en un viaje
    List<Wallet> findByTripIdAndUserId(Long tripId, Long userId);
    
    // Buscar todas las billeteras de un usuario
    List<Wallet> findByUserId(Long userId);
}

