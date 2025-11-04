package TravelMate_Backend.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripLocationUpdateRequest {
    @NotBlank(message = "El origen es obligatorio")
    private String origin;
    
    @NotBlank(message = "El destino es obligatorio")
    private String destination;
    
    @JsonProperty("originAddress")
    private String originAddress;
    
    @JsonProperty("destinationAddress")
    private String destinationAddress;
    
    @JsonProperty("originCoords")
    private Coords originCoords;
    
    @JsonProperty("destinationCoords")
    private Coords destinationCoords;
    
    @JsonProperty("vehicle")
    private String vehicle; // auto, avion, etc.

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coords {
        @JsonProperty("lat")
        private double lat;
        
        @JsonProperty("lng")
        private double lng;
    }
}

