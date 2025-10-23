package TravelMate_Backend.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipCreateRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String name;
    
    private String description;
    
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    private String address;
    
    @NotNull(message = "La latitud es obligatoria")
    private Double latitude;
    
    @NotNull(message = "La longitud es obligatoria")
    private Double longitude;
    
    private Double rating;
    
    private Double distanceKm;
    
    @NotBlank(message = "El tipo de tip es obligatorio")
    private String tipType; // restaurant, lodging, attraction, gas_station
    
    private String tipIcon; // 🍽️, 🏨, 🎯, ⛽
    
    private List<String> types;
}
