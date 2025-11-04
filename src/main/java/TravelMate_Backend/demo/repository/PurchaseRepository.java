package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    // Buscar todas las compras de un viaje
    List<Purchase> findByTripId(Long tripId);
    
    // Buscar compras generales de un viaje
    List<Purchase> findByTripIdAndIsGeneralTrue(Long tripId);
    
    // Buscar compras individuales de un usuario en un viaje
    List<Purchase> findByTripIdAndUserIdAndIsGeneralFalse(Long tripId, Long userId);
    
    // Buscar compra por ID y tripId
    Optional<Purchase> findByIdAndTripId(Long id, Long tripId);
    
    // Buscar todas las compras individuales de un usuario en un viaje
    List<Purchase> findByTripIdAndUserId(Long tripId, Long userId);
}

