package TravelMate_Backend.demo.dto;

import TravelMate_Backend.demo.model.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripCreate {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String name;

    private String description;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateI;

    @NotNull(message = "La fecha de fin es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateF;

    @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
    private BigDecimal cost;

    @NotNull(message = "La moneda es obligatoria")
    private Currency currency;

    private String status;
    
    // Campos para origen y destino
    private String origin;
    private String destination;
    private String vehicle;
    
    // Campos para Google Maps
    private String originAddress;
    private String destinationAddress;
    private Coords originCoords;
    private Coords destinationCoords;
    private String transportMode;
    
    // Clase interna para coordenadas
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coords {
        private double lat;
        private double lng;
    }

    @AssertTrue(message = "La fecha de fin debe ser posterior a la fecha de inicio")
    public boolean isValidDateRange() {
        if (dateI == null || dateF == null) {
            return true;
        }
        return dateF.isAfter(dateI);
    }
}
