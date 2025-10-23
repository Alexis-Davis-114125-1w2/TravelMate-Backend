package TravelMate_Backend.demo.repository;

import TravelMate_Backend.demo.model.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {
    
    /**
     * Buscar todos los tips de un viaje específico
     */
    List<Tip> findByTripIdOrderByCreatedAtDesc(Long tripId);
    
    /**
     * Buscar tips por tipo en un viaje específico
     */
    List<Tip> findByTripIdAndTipTypeOrderByCreatedAtDesc(Long tripId, String tipType);
    
    /**
     * Buscar tips creados por un usuario específico en un viaje
     */
    List<Tip> findByTripIdAndCreatedByOrderByCreatedAtDesc(Long tripId, String createdBy);
    
    /**
     * Contar el número de tips en un viaje
     */
    long countByTripId(Long tripId);
    
    /**
     * Buscar tips por tipo y ordenar por distancia
     */
    @Query("SELECT t FROM Tip t WHERE t.trip.id = :tripId AND t.tipType = :tipType ORDER BY t.distanceKm ASC")
    List<Tip> findByTripIdAndTipTypeOrderByDistanceAsc(@Param("tripId") Long tripId, @Param("tipType") String tipType);
    
    /**
     * Buscar tips cercanos a una ubicación específica
     */
    @Query("SELECT t FROM Tip t WHERE t.trip.id = :tripId AND " +
           "SQRT(POWER(t.latitude - :latitude, 2) + POWER(t.longitude - :longitude, 2)) <= :radiusKm " +
           "ORDER BY SQRT(POWER(t.latitude - :latitude, 2) + POWER(t.longitude - :longitude, 2)) ASC")
    List<Tip> findNearbyTips(@Param("tripId") Long tripId, 
                            @Param("latitude") Double latitude, 
                            @Param("longitude") Double longitude, 
                            @Param("radiusKm") Double radiusKm);
    
    /**
     * Eliminar todos los tips de un viaje
     */
    void deleteByTripId(Long tripId);
    
    /**
     * Eliminar tips creados por un usuario específico en un viaje
     */
    void deleteByTripIdAndCreatedBy(Long tripId, String createdBy);
}
