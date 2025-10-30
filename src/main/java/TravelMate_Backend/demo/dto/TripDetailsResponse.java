package TravelMate_Backend.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDetailsResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate dateI;
    private LocalDate dateF;
    private BigDecimal cost;
    private String joinCode;
    private String status;
    private String image; // Base64 encoded image or image type
    
    // Campos de origen y destino
    private String origin;
    private String destination;
    private String vehicle;
    
    // Coordenadas de origen
    private Double originLatitude;
    private Double originLongitude;
    private String originAddress;
    
    // Coordenadas de destino
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationAddress;
    
    // Información de transporte
    private String transportMode;
    
    // Lista de participantes
    private List<ParticipantInfo> participants;
    private Long createBy;
    private Set<Long> adminIds;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private Long id;
        private String name;
        private String email;
        private String profilePicture;
    }
}
