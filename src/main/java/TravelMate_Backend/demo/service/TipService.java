package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.model.Tip;
import TravelMate_Backend.demo.model.Trip;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.repository.TipRepository;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TipService {
    
    @Autowired
    private TipRepository tipRepository;
    
    @Autowired
    private TripRepository tripRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Crear un nuevo tip para un viaje
     */
    public Tip createTip(Long tripId, String userEmail, Tip tipData) {
        System.out.println("TipService.createTip - Iniciando creación de tip");
        System.out.println("TripId: " + tripId + ", UserEmail: " + userEmail);
        
        // Verificar que el viaje existe
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            System.err.println("TipService.createTip - Viaje no encontrado: " + tripId);
            throw new RuntimeException("Viaje no encontrado");
        }
        
        Trip trip = tripOpt.get();
        System.out.println("TipService.createTip - Viaje encontrado: " + trip.getName());
        
        // Verificar que el usuario existe
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            System.err.println("TipService.createTip - Usuario no encontrado: " + userEmail);
            throw new RuntimeException("Usuario no encontrado");
        }
        
        User user = userOpt.get();
        System.out.println("TipService.createTip - Usuario encontrado: " + user.getName() + " (ID: " + user.getId() + ")");
        
        // Verificar que el usuario está en el viaje usando una consulta directa
        boolean userInTrip = tripRepository.existsByIdAndUsersId(tripId, user.getId());
        System.out.println("TipService.createTip - Usuario en viaje: " + userInTrip);
        
        if (!userInTrip) {
            System.err.println("TipService.createTip - El usuario " + userEmail + " no está en el viaje " + tripId);
            throw new RuntimeException("El usuario no está en este viaje");
        }
        
        // Configurar el tip
        tipData.setTrip(trip);
        tipData.setCreatedBy(userEmail);
        tipData.setCreatedAt(LocalDateTime.now());
        
        System.out.println("TipService.createTip - Guardando tip en base de datos");
        Tip savedTip = tipRepository.save(tipData);
        System.out.println("TipService.createTip - Tip guardado exitosamente con ID: " + savedTip.getId());
        
        return savedTip;
    }
    
    /**
     * Obtener todos los tips de un viaje
     */
    public List<Tip> getTipsByTripId(Long tripId) {
        return tipRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }
    
    /**
     * Obtener tips por tipo en un viaje
     */
    public List<Tip> getTipsByTripIdAndType(Long tripId, String tipType) {
        return tipRepository.findByTripIdAndTipTypeOrderByCreatedAtDesc(tripId, tipType);
    }
    
    /**
     * Obtener tips creados por un usuario en un viaje
     */
    public List<Tip> getTipsByTripIdAndUser(Long tripId, String userEmail) {
        return tipRepository.findByTripIdAndCreatedByOrderByCreatedAtDesc(tripId, userEmail);
    }
    
    /**
     * Buscar tips cercanos a una ubicación
     */
    public List<Tip> getNearbyTips(Long tripId, Double latitude, Double longitude, Double radiusKm) {
        return tipRepository.findNearbyTips(tripId, latitude, longitude, radiusKm);
    }
    
    /**
     * Obtener un tip por ID
     */
    public Optional<Tip> getTipById(Long tipId) {
        return tipRepository.findById(tipId);
    }
    
    /**
     * Actualizar un tip
     */
    public Tip updateTip(Long tipId, String userEmail, Tip updatedTip) {
        Optional<Tip> tipOpt = tipRepository.findById(tipId);
        if (tipOpt.isEmpty()) {
            throw new RuntimeException("Tip no encontrado");
        }
        
        Tip existingTip = tipOpt.get();
        
        // Verificar que el usuario es el creador del tip
        if (!existingTip.getCreatedBy().equals(userEmail)) {
            throw new RuntimeException("No tienes permisos para modificar este tip");
        }
        
        // Actualizar campos
        existingTip.setName(updatedTip.getName());
        existingTip.setDescription(updatedTip.getDescription());
        existingTip.setAddress(updatedTip.getAddress());
        existingTip.setLatitude(updatedTip.getLatitude());
        existingTip.setLongitude(updatedTip.getLongitude());
        existingTip.setRating(updatedTip.getRating());
        existingTip.setDistanceKm(updatedTip.getDistanceKm());
        existingTip.setTipType(updatedTip.getTipType());
        existingTip.setTipIcon(updatedTip.getTipIcon());
        existingTip.setTypes(updatedTip.getTypes());
        
        return tipRepository.save(existingTip);
    }
    
    /**
     * Eliminar un tip
     */
    public void deleteTip(Long tipId, String userEmail) {
        Optional<Tip> tipOpt = tipRepository.findById(tipId);
        if (tipOpt.isEmpty()) {
            throw new RuntimeException("Tip no encontrado");
        }
        
        Tip tip = tipOpt.get();
        
        // Verificar que el usuario es el creador del tip
        if (!tip.getCreatedBy().equals(userEmail)) {
            throw new RuntimeException("No tienes permisos para eliminar este tip");
        }
        
        tipRepository.delete(tip);
    }
    
    /**
     * Eliminar todos los tips de un viaje
     */
    public void deleteAllTipsByTripId(Long tripId) {
        tipRepository.deleteByTripId(tripId);
    }
    
    /**
     * Eliminar todos los tips de un usuario en un viaje
     */
    public void deleteAllTipsByTripIdAndUser(Long tripId, String userEmail) {
        tipRepository.deleteByTripIdAndCreatedBy(tripId, userEmail);
    }
    
    /**
     * Contar tips en un viaje
     */
    public long countTipsByTripId(Long tripId) {
        return tipRepository.countByTripId(tripId);
    }
    
    /**
     * Verificar si un usuario puede acceder a un tip
     */
    public boolean canUserAccessTip(Long tipId, String userEmail) {
        Optional<Tip> tipOpt = tipRepository.findById(tipId);
        if (tipOpt.isEmpty()) {
            return false;
        }
        
        Tip tip = tipOpt.get();
        Trip trip = tip.getTrip();
        
        // Verificar que el usuario está en el viaje
        return trip.getUsers().stream()
            .anyMatch(user -> user.getEmail().equals(userEmail));
    }
}
