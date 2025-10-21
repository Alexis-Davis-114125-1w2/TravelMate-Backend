package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.ApiResponse;
import TravelMate_Backend.demo.dto.TripCreate;
import TravelMate_Backend.demo.model.Trip;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.service.TripServices;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/trips")
public class TripController {
    @Autowired
    private TripServices tripServices;
    @GetMapping("/get/{userId}")
    public ResponseEntity<?> getTripsUser( @PathVariable Long userId) {
        List<Trip> trips = tripServices.getUserTrips(userId);
        if (trips.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trips);
    }
    @PostMapping("/add")
    public ResponseEntity<?> addTrip(@RequestPart("trip") TripCreate trip,
                                     @RequestPart(value = "image", required = false) MultipartFile imageFile,
                                     @RequestParam Long userId) {
        try {
            System.out.println("TripController.addTrip - Recibiendo request");
            System.out.println("Trip data: " + trip);
            System.out.println("User ID: " + userId);
            
            Trip trip1 = tripServices.createTrip(trip, userId, imageFile);
            System.out.println("Trip creado exitosamente con ID: " + trip1.getId());
            
            return ResponseEntity.ok(trip1);
        } catch (Exception e) {
            System.err.println("Error en TripController.addTrip: " + e.getMessage());
            e.printStackTrace();
            
            // Devolver un JSON con el error
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al crear el viaje");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{id}/users/{userId}")
    public ResponseEntity<?> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody TripCreate tripDto, @PathVariable Long userId) {
        try {
            Trip updatedTrip = tripServices.updateTrip(id, tripDto, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Viaje actualizado exitosamente",
                    "data", updatedTrip
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrip(
            @PathVariable Long id,
            @RequestParam Long userId){
        try {

            tripServices.deleteTrip(id, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Viaje eliminado exitosamente"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{tripId}/users/{userId}")
    public ResponseEntity<?> addUserToTrip(
            @PathVariable Long tripId,
            @PathVariable Long userId,
            @PathVariable Long tripUserId) {
        try {

            tripServices.addUserToTrip(userId, tripId, tripUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario agregado al viaje exitosamente"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{tripId}/users/{userId}")
    public ResponseEntity<?> removeUserFromTrip(
            @PathVariable Long tripId,
            @PathVariable Long userId,
            @PathVariable Long tripUserId) {
        try {

            tripServices.removeUserFromTrip(userId, tripId, tripUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario removido del viaje exitosamente"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<?> getTripParticipants(
            @PathVariable Long id,
            @RequestParam Long userId) {
        try {

            List<User> participants = tripServices.getTripParticipants(id, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", participants,
                    "total", participants.size()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse> joinTripByCode(
            @RequestParam String code,
            @RequestParam Long userId) {
        try {
            tripServices.joinTripByCode(code, userId);

            ApiResponse response = new ApiResponse(
                    true,
                    "Usuario unido exitosamente al viaje con código " + code
            );
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            ApiResponse response = new ApiResponse(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
