package TravelMate_Backend.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipResponse {
    
    private Long id;
    private String name;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double rating;
    private Double distanceKm;
    private String tipType;
    private String tipIcon;
    private List<String> types;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long tripId;
    
}
