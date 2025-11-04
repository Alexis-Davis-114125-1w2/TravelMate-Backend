package TravelMate_Backend.demo.service;

import TravelMate_Backend.demo.dto.PurchaseCreateRequest;
import TravelMate_Backend.demo.dto.PurchaseResponse;
import TravelMate_Backend.demo.dto.PurchaseUpdateRequest;
import TravelMate_Backend.demo.model.*;
import TravelMate_Backend.demo.repository.PurchaseRepository;
import TravelMate_Backend.demo.repository.TripRepository;
import TravelMate_Backend.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Crear compra general del viaje
     */
    public PurchaseResponse createGeneralPurchase(Long tripId, Long createdByUserId, PurchaseCreateRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        // Verificar que el usuario creador existe
        userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado"));

        Purchase purchase = new Purchase();
        purchase.setTrip(trip);
        purchase.setUser(null); // null para compras generales
        purchase.setDescription(request.getDescription());
        purchase.setPrice(request.getPrice());
        purchase.setCurrency(request.getCurrency());
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setIsGeneral(true);
        purchase.setCreatedBy(createdByUserId); // Usuario que creó la compra

        purchase = purchaseRepository.save(purchase);
        return convertToResponse(purchase);
    }

    /**
     * Crear compra individual de un usuario
     */
    public PurchaseResponse createIndividualPurchase(Long tripId, Long userId, Long createdByUserId, PurchaseCreateRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el usuario creador existe
        userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado"));

        // Verificar que el usuario pertenece al viaje
        boolean userBelongsToTrip = tripRepository.existsByIdAndUsersId(tripId, userId);
        if (!userBelongsToTrip) {
            throw new RuntimeException("El usuario no pertenece a este viaje");
        }

        Purchase purchase = new Purchase();
        purchase.setTrip(trip);
        purchase.setUser(user); // Usuario dueño de la compra
        purchase.setDescription(request.getDescription());
        purchase.setPrice(request.getPrice());
        purchase.setCurrency(request.getCurrency());
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setIsGeneral(false);
        purchase.setCreatedBy(createdByUserId); // Usuario que creó la compra

        purchase = purchaseRepository.save(purchase);
        return convertToResponse(purchase);
    }

    /**
     * Obtener todas las compras de un viaje
     */
    public List<PurchaseResponse> getAllPurchasesByTrip(Long tripId) {
        List<Purchase> purchases = purchaseRepository.findByTripId(tripId);
        return purchases.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener compras generales de un viaje
     */
    public List<PurchaseResponse> getGeneralPurchases(Long tripId) {
        List<Purchase> purchases = purchaseRepository.findByTripIdAndIsGeneralTrue(tripId);
        return purchases.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener compras individuales de un usuario en un viaje
     */
    public List<PurchaseResponse> getIndividualPurchases(Long tripId, Long userId) {
        List<Purchase> purchases = purchaseRepository.findByTripIdAndUserIdAndIsGeneralFalse(tripId, userId);
        return purchases.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener compra por ID
     */
    public PurchaseResponse getPurchaseById(Long purchaseId, Long tripId) {
        Purchase purchase = purchaseRepository.findByIdAndTripId(purchaseId, tripId)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));
        return convertToResponse(purchase);
    }

    /**
     * Actualizar compra general
     */
    public PurchaseResponse updateGeneralPurchase(Long tripId, Long purchaseId, PurchaseUpdateRequest request) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        Purchase purchase = purchaseRepository.findByIdAndTripId(purchaseId, tripId)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));

        if (!purchase.getIsGeneral()) {
            throw new RuntimeException("Esta compra no es una compra general");
        }

        purchase.setDescription(request.getDescription());
        purchase.setPrice(request.getPrice());
        purchase.setCurrency(request.getCurrency());
        purchase.setPurchaseDate(request.getPurchaseDate());

        purchase = purchaseRepository.save(purchase);
        return convertToResponse(purchase);
    }

    /**
     * Actualizar compra individual
     */
    public PurchaseResponse updateIndividualPurchase(Long tripId, Long userId, Long purchaseId, PurchaseUpdateRequest request) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Purchase purchase = purchaseRepository.findByIdAndTripId(purchaseId, tripId)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));

        if (purchase.getIsGeneral()) {
            throw new RuntimeException("Esta compra no es una compra individual");
        }

        // Verificar que la compra pertenece al usuario
        if (purchase.getUser() == null || !purchase.getUser().getId().equals(userId)) {
            throw new RuntimeException("Esta compra no pertenece al usuario especificado");
        }

        purchase.setDescription(request.getDescription());
        purchase.setPrice(request.getPrice());
        purchase.setCurrency(request.getCurrency());
        purchase.setPurchaseDate(request.getPurchaseDate());

        purchase = purchaseRepository.save(purchase);
        return convertToResponse(purchase);
    }

    /**
     * Eliminar compra
     */
    public void deletePurchase(Long tripId, Long purchaseId) {
        Purchase purchase = purchaseRepository.findByIdAndTripId(purchaseId, tripId)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));
        purchaseRepository.delete(purchase);
    }

    /**
     * Convertir Purchase a PurchaseResponse
     */
    private PurchaseResponse convertToResponse(Purchase purchase) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchase.getId());
        response.setTripId(purchase.getTrip().getId());
        response.setDescription(purchase.getDescription());
        response.setPrice(purchase.getPrice());
        response.setCurrency(purchase.getCurrency());
        response.setCurrencySymbol(purchase.getCurrency().getSymbol());
        response.setPurchaseDate(purchase.getPurchaseDate());
        response.setIsGeneral(purchase.getIsGeneral());
        response.setCreatedBy(purchase.getCreatedBy());
        response.setCreatedAt(purchase.getCreatedAt());
        response.setUpdatedAt(purchase.getUpdatedAt());

        // Información del usuario dueño de la compra (si es individual)
        if (purchase.getUser() != null) {
            response.setUserId(purchase.getUser().getId());
            response.setUserName(purchase.getUser().getName());
            response.setUserEmail(purchase.getUser().getEmail());
        }

        // Información del usuario que creó la compra
        if (purchase.getCreatedBy() != null) {
            try {
                User createdByUser = userRepository.findById(purchase.getCreatedBy())
                        .orElse(null);
                if (createdByUser != null) {
                    response.setCreatedByName(createdByUser.getName());
                    response.setCreatedByEmail(createdByUser.getEmail());
                }
            } catch (Exception e) {
                // Si no se puede obtener el usuario, simplemente no se asigna
                // El createdBy ya está asignado arriba
            }
        }

        return response;
    }
}

