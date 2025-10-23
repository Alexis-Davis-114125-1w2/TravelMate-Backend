package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.ApiResponse;
import TravelMate_Backend.demo.dto.TipCreateRequest;
import TravelMate_Backend.demo.dto.TipResponse;
import TravelMate_Backend.demo.model.Tip;
import TravelMate_Backend.demo.service.TipService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/tips")
public class TipController {
    
    @Autowired
    private TipService tipService;
    
    /**
     * Crear un nuevo tip para un viaje
     */
    @PostMapping("/trip/{tripId}")
    public ResponseEntity<?> createTip(
            @PathVariable Long tripId,
            @RequestParam String userEmail,
            @Valid @RequestBody TipCreateRequest tipRequest) {
        try {
            System.out.println("TipController.createTip - Iniciando creación de tip");
            System.out.println("TripId: " + tripId);
            System.out.println("UserEmail: " + userEmail);
            System.out.println("TipRequest: " + tipRequest);
            
            Tip tip = new Tip(
                tipRequest.getName(),
                tipRequest.getDescription(),
                tipRequest.getAddress(),
                tipRequest.getLatitude(),
                tipRequest.getLongitude(),
                tipRequest.getRating(),
                tipRequest.getDistanceKm(),
                tipRequest.getTipType(),
                tipRequest.getTipIcon(),
                tipRequest.getTypes(),
                userEmail,
                null // Se establecerá en el servicio
            );
            
            System.out.println("TipController.createTip - Tip creado, llamando al servicio");
            Tip createdTip = tipService.createTip(tripId, userEmail, tip);
            System.out.println("TipController.createTip - Tip guardado exitosamente con ID: " + createdTip.getId());
            
            TipResponse response = convertToResponse(createdTip);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Tip creado exitosamente", response));
        } catch (Exception e) {
            System.err.println("TipController.createTip - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al crear tip: " + e.getMessage(), null));
        }
    }
    
    /**
     * Obtener todos los tips de un viaje
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getTipsByTripId(@PathVariable Long tripId) {
        try {
            List<Tip> tips = tipService.getTipsByTripId(tripId);
            List<TipResponse> responses = tips.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Tips obtenidos exitosamente", responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al obtener tips: " + e.getMessage(), null));
        }
    }
    
    /**
     * Obtener tips por tipo en un viaje
     */
    @GetMapping("/trip/{tripId}/type/{tipType}")
    public ResponseEntity<?> getTipsByTripIdAndType(
            @PathVariable Long tripId,
            @PathVariable String tipType) {
        try {
            List<Tip> tips = tipService.getTipsByTripIdAndType(tripId, tipType);
            List<TipResponse> responses = tips.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Tips obtenidos exitosamente", responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al obtener tips: " + e.getMessage(), null));
        }
    }
    
    /**
     * Obtener tips cercanos a una ubicación
     */
    @GetMapping("/trip/{tripId}/nearby")
    public ResponseEntity<?> getNearbyTips(
            @PathVariable Long tripId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        try {
            List<Tip> tips = tipService.getNearbyTips(tripId, latitude, longitude, radiusKm);
            List<TipResponse> responses = tips.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Tips cercanos obtenidos exitosamente", responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al obtener tips cercanos: " + e.getMessage(), null));
        }
    }
    
    /**
     * Obtener un tip por ID
     */
    @GetMapping("/{tipId}")
    public ResponseEntity<?> getTipById(@PathVariable Long tipId) {
        try {
            return tipService.getTipById(tipId)
                .map(tip -> ResponseEntity.ok(new ApiResponse(true, "Tip obtenido exitosamente", convertToResponse(tip))))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al obtener tip: " + e.getMessage(), null));
        }
    }
    
    /**
     * Actualizar un tip
     */
    @PutMapping("/{tipId}")
    public ResponseEntity<?> updateTip(
            @PathVariable Long tipId,
            @RequestParam String userEmail,
            @Valid @RequestBody TipCreateRequest tipRequest) {
        try {
            Tip updatedTip = new Tip(
                tipRequest.getName(),
                tipRequest.getDescription(),
                tipRequest.getAddress(),
                tipRequest.getLatitude(),
                tipRequest.getLongitude(),
                tipRequest.getRating(),
                tipRequest.getDistanceKm(),
                tipRequest.getTipType(),
                tipRequest.getTipIcon(),
                tipRequest.getTypes(),
                userEmail,
                null
            );
            
            Tip result = tipService.updateTip(tipId, userEmail, updatedTip);
            TipResponse response = convertToResponse(result);
            
            return ResponseEntity.ok(new ApiResponse(true, "Tip actualizado exitosamente", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al actualizar tip: " + e.getMessage(), null));
        }
    }
    
    /**
     * Eliminar un tip
     */
    @DeleteMapping("/{tipId}")
    public ResponseEntity<?> deleteTip(
            @PathVariable Long tipId,
            @RequestParam String userEmail) {
        try {
            tipService.deleteTip(tipId, userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Tip eliminado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al eliminar tip: " + e.getMessage(), null));
        }
    }
    
    /**
     * Eliminar todos los tips de un viaje
     */
    @DeleteMapping("/trip/{tripId}")
    public ResponseEntity<?> deleteAllTipsByTripId(@PathVariable Long tripId) {
        try {
            tipService.deleteAllTipsByTripId(tripId);
            return ResponseEntity.ok(new ApiResponse(true, "Todos los tips del viaje eliminados exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al eliminar tips: " + e.getMessage(), null));
        }
    }
    
    /**
     * Obtener estadísticas de tips de un viaje
     */
    @GetMapping("/trip/{tripId}/stats")
    public ResponseEntity<?> getTipStats(@PathVariable Long tripId) {
        try {
            long totalTips = tipService.countTipsByTripId(tripId);
            List<Tip> allTips = tipService.getTipsByTripId(tripId);
            
            Map<String, Long> tipsByType = allTips.stream()
                .collect(Collectors.groupingBy(
                    tip -> tip.getTipType() != null ? tip.getTipType() : "unknown",
                    Collectors.counting()
                ));
            
            Map<String, Object> stats = Map.of(
                "totalTips", totalTips,
                "tipsByType", tipsByType
            );
            
            return ResponseEntity.ok(new ApiResponse(true, "Estadísticas obtenidas exitosamente", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Error al obtener estadísticas: " + e.getMessage(), null));
        }
    }
    
    /**
     * Convertir Tip a TipResponse
     */
    private TipResponse convertToResponse(Tip tip) {
        return new TipResponse(
            tip.getId(),
            tip.getName(),
            tip.getDescription(),
            tip.getAddress(),
            tip.getLatitude(),
            tip.getLongitude(),
            tip.getRating(),
            tip.getDistanceKm(),
            tip.getTipType(),
            tip.getTipIcon(),
            tip.getTypes(),
            tip.getCreatedAt(),
            tip.getCreatedBy(),
            tip.getTrip() != null ? tip.getTrip().getId() : null
        );
    }
}
