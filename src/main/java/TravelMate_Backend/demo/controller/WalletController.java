package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.ApiResponse;
import TravelMate_Backend.demo.dto.WalletResponse;
import TravelMate_Backend.demo.dto.WalletUpdateRequest;
import TravelMate_Backend.demo.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    /**
     * Obtener todas las billeteras de un viaje
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getAllWalletsByTrip(@PathVariable Long tripId) {
        try {
            List<WalletResponse> wallets = walletService.getAllWalletsByTrip(tripId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Billeteras obtenidas exitosamente", wallets));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener billeteras: " + e.getMessage(), null));
        }
    }

    /**
     * Obtener billetera general de un viaje
     */
    @GetMapping("/trip/{tripId}/general")
    public ResponseEntity<ApiResponse<WalletResponse>> getGeneralWallet(@PathVariable Long tripId) {
        try {
            WalletResponse wallet = walletService.getGeneralWallet(tripId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Billetera general obtenida exitosamente", wallet));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener billetera general: " + e.getMessage(), null));
        }
    }

    /**
     * Obtener billetera individual de un usuario en un viaje
     */
    @GetMapping("/trip/{tripId}/individual/{userId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getIndividualWallet(
            @PathVariable Long tripId,
            @PathVariable Long userId) {
        try {
            WalletResponse wallet = walletService.getIndividualWallet(tripId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Billetera individual obtenida exitosamente", wallet));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al obtener billetera individual: " + e.getMessage(), null));
        }
    }

    /**
     * Actualizar billetera general de un viaje
     */
    @PutMapping("/trip/{tripId}/general")
    public ResponseEntity<ApiResponse<WalletResponse>> updateGeneralWallet(
            @PathVariable Long tripId,
            @Valid @RequestBody WalletUpdateRequest request) {
        try {
            WalletResponse wallet = walletService.updateGeneralWallet(tripId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Billetera general actualizada exitosamente", wallet));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al actualizar billetera";
            }
            e.printStackTrace(); // Log para debugging
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al actualizar billetera general: " + errorMessage, null));
        }
    }

    /**
     * Actualizar billetera individual de un usuario en un viaje
     */
    @PutMapping("/trip/{tripId}/individual/{userId}")
    public ResponseEntity<ApiResponse<WalletResponse>> updateIndividualWallet(
            @PathVariable Long tripId,
            @PathVariable Long userId,
            @Valid @RequestBody WalletUpdateRequest request) {
        try {
            WalletResponse wallet = walletService.updateIndividualWallet(tripId, userId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Billetera individual actualizada exitosamente", wallet));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + ": Error desconocido al actualizar billetera";
            }
            e.printStackTrace(); // Log para debugging
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error al actualizar billetera individual: " + errorMessage, null));
        }
    }
}

