package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.ApiResponse;
import TravelMate_Backend.demo.dto.PurchaseCreateRequest;
import TravelMate_Backend.demo.dto.PurchaseResponse;
import TravelMate_Backend.demo.dto.PurchaseUpdateRequest;
import TravelMate_Backend.demo.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    /**
     * Crear compra general del viaje
     */
    @PostMapping("/trip/{tripId}/general")
    public ResponseEntity<ApiResponse<PurchaseResponse>> createGeneralPurchase(
            @PathVariable Long tripId,
            @RequestHeader("User-Id") Long createdByUserId,
            @Valid @RequestBody PurchaseCreateRequest request) {
        try {
            PurchaseResponse purchase = purchaseService.createGeneralPurchase(tripId, createdByUserId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compra general creada exitosamente", purchase));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al crear compra";
            }
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al crear compra general: " + errorMessage, null));
        }
    }

    /**
     * Crear compra individual de un usuario
     */
    @PostMapping("/trip/{tripId}/individual/{userId}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> createIndividualPurchase(
            @PathVariable Long tripId,
            @PathVariable Long userId,
            @RequestHeader("User-Id") Long createdByUserId,
            @Valid @RequestBody PurchaseCreateRequest request) {
        try {
            PurchaseResponse purchase = purchaseService.createIndividualPurchase(tripId, userId, createdByUserId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compra individual creada exitosamente", purchase));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al crear compra";
            }
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al crear compra individual: " + errorMessage, null));
        }
    }

    /**
     * Obtener todas las compras de un viaje
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getAllPurchasesByTrip(@PathVariable Long tripId) {
        try {
            List<PurchaseResponse> purchases = purchaseService.getAllPurchasesByTrip(tripId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compras obtenidas exitosamente", purchases));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido";
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener compras: " + errorMessage, null));
        }
    }

    /**
     * Obtener compras generales de un viaje
     */
    @GetMapping("/trip/{tripId}/general")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getGeneralPurchases(@PathVariable Long tripId) {
        try {
            List<PurchaseResponse> purchases = purchaseService.getGeneralPurchases(tripId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compras generales obtenidas exitosamente", purchases));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido";
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener compras generales: " + errorMessage, null));
        }
    }

    /**
     * Obtener compras individuales de un usuario en un viaje
     */
    @GetMapping("/trip/{tripId}/individual/{userId}")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getIndividualPurchases(
            @PathVariable Long tripId,
            @PathVariable Long userId) {
        try {
            List<PurchaseResponse> purchases = purchaseService.getIndividualPurchases(tripId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compras individuales obtenidas exitosamente", purchases));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido";
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener compras individuales: " + errorMessage, null));
        }
    }

    /**
     * Obtener compra por ID
     */
    @GetMapping("/{purchaseId}/trip/{tripId}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> getPurchaseById(
            @PathVariable Long purchaseId,
            @PathVariable Long tripId) {
        try {
            PurchaseResponse purchase = purchaseService.getPurchaseById(purchaseId, tripId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compra obtenida exitosamente", purchase));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido";
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener compra: " + errorMessage, null));
        }
    }

    /**
     * Actualizar compra general
     */
    @PutMapping("/trip/{tripId}/general/{purchaseId}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updateGeneralPurchase(
            @PathVariable Long tripId,
            @PathVariable Long purchaseId,
            @Valid @RequestBody PurchaseUpdateRequest request) {
        try {
            PurchaseResponse purchase = purchaseService.updateGeneralPurchase(tripId, purchaseId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compra general actualizada exitosamente", purchase));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al actualizar compra";
            }
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al actualizar compra general: " + errorMessage, null));
        }
    }

    /**
     * Actualizar compra individual
     */
    @PutMapping("/trip/{tripId}/individual/{userId}/{purchaseId}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updateIndividualPurchase(
            @PathVariable Long tripId,
            @PathVariable Long userId,
            @PathVariable Long purchaseId,
            @Valid @RequestBody PurchaseUpdateRequest request) {
        try {
            PurchaseResponse purchase = purchaseService.updateIndividualPurchase(tripId, userId, purchaseId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compra individual actualizada exitosamente", purchase));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al actualizar compra";
            }
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al actualizar compra individual: " + errorMessage, null));
        }
    }

    /**
     * Eliminar compra
     */
    @DeleteMapping("/trip/{tripId}/{purchaseId}")
    public ResponseEntity<ApiResponse<Void>> deletePurchase(
            @PathVariable Long tripId,
            @PathVariable Long purchaseId) {
        try {
            purchaseService.deletePurchase(tripId, purchaseId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Compra eliminada exitosamente", null));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al eliminar compra";
            }
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al eliminar compra: " + errorMessage, null));
        }
    }
}

