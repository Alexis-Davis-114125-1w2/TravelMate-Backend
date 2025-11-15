package TravelMate_Backend.demo.controller;

import TravelMate_Backend.demo.dto.ApiResponse;
import TravelMate_Backend.demo.dto.TripCreate;
import TravelMate_Backend.demo.dto.TripDetailsResponse;
import TravelMate_Backend.demo.model.Trip;
import TravelMate_Backend.demo.model.User;
import TravelMate_Backend.demo.service.TripServices;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/trips")
public class TripController {
    @Autowired
    private TripServices tripServices;
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTripsUser( @PathVariable Long userId) {
        List<Trip> trips = tripServices.getUserTrips(userId);
        if (trips.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripById(@PathVariable Long id, @RequestParam Long userId) {
        try {
            System.out.println("TripController.getTripById - Recibiendo request para tripId: " + id + ", userId: " + userId);
            TripDetailsResponse tripDetails = tripServices.getTripDetails(id, userId);
            System.out.println("TripController.getTripById - Trip details obtenidos exitosamente");
            return ResponseEntity.ok(tripDetails);
        } catch (RuntimeException e) {
            System.err.println("TripController.getTripById - Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}/{userId}")
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @DeleteMapping("/{tripId}/users/{eliminateUserId}/{tripUserId}")
    public ResponseEntity<?> removeUserFromTrip(
            @PathVariable Long tripId,
            @PathVariable Long eliminateUserId,
            @PathVariable Long tripUserId) {
        try {

            tripServices.removeUserFromTrip(eliminateUserId, tripId, tripUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario removido del viaje exitosamente"
            ));

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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
                    "Usuario unido exitosamente al viaje con código " + code,
                    null
            );
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            ApiResponse response = new ApiResponse(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    @PutMapping("/{tripId}/{userId}/admins/add/{adminId}")
    public ResponseEntity<Trip> addAdminId(
            @PathVariable Long userId,
            @PathVariable Long adminId,
            @PathVariable Long tripId) {
        Trip trip = tripServices.addAdminId(userId, adminId,tripId);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{tripId}/{userId}/admins/remove/{adminId}")
    public ResponseEntity<?> removeAdminId(
            @PathVariable Long userId,
            @PathVariable Long adminId,
            @PathVariable Long tripId) {
        ResponseEntity<?> trip = tripServices.removeAdminId(userId, adminId,tripId);
        return trip;
    }

    @PutMapping("/{tripId}/dates")
    public ResponseEntity<?> updateTripDates(
            @PathVariable Long tripId,
            @RequestParam Long userId,
            @Valid @RequestBody TravelMate_Backend.demo.dto.TripDatesUpdateRequest request) {
        try {
            System.out.println("TripController.updateTripDates - Recibiendo request para tripId: " + tripId + ", userId: " + userId);
            System.out.println("TripController.updateTripDates - Request data: dateI=" + request.getDateI() + ", dateF=" + request.getDateF());
            
            Trip updatedTrip = tripServices.updateTripDates(tripId, request, userId);
            System.out.println("TripController.updateTripDates - Trip actualizado exitosamente");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Fechas del viaje actualizadas exitosamente",
                    "data", updatedTrip
            ));
        } catch (RuntimeException e) {
            System.err.println("TripController.updateTripDates - Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{tripId}/locations")
    public ResponseEntity<?> updateTripLocations(
            @PathVariable Long tripId,
            @RequestParam Long userId,
            @Valid @RequestBody TravelMate_Backend.demo.dto.TripLocationUpdateRequest request) {
        try {
            System.out.println("TripController.updateTripLocations - Recibiendo request para tripId: " + tripId + ", userId: " + userId);
            System.out.println("TripController.updateTripLocations - Request data: origin=" + request.getOrigin() + ", destination=" + request.getDestination());
            System.out.println("TripController.updateTripLocations - OriginCoords: " + (request.getOriginCoords() != null ? request.getOriginCoords().getLat() + "," + request.getOriginCoords().getLng() : "null"));
            System.out.println("TripController.updateTripLocations - DestinationCoords: " + (request.getDestinationCoords() != null ? request.getDestinationCoords().getLat() + "," + request.getDestinationCoords().getLng() : "null"));
            
            Trip updatedTrip = tripServices.updateTripLocations(tripId, request, userId);
            System.out.println("TripController.updateTripLocations - Trip actualizado exitosamente");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Origen y destino del viaje actualizados exitosamente",
                    "data", updatedTrip
            ));
        } catch (RuntimeException e) {
            System.err.println("TripController.updateTripLocations - Error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        errorResponse.put("message", "Error de validación: " + String.join(", ", errors));
        errorResponse.put("errors", errors);
        
        System.err.println("TripController - Error de validación: " + errors);
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
