package TravelMate_Backend.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        System.out.println("HealthController.health - Endpoint llamado");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "Backend is running",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}
