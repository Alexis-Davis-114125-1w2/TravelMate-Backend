package TravelMate_Backend.demo.dto;

import TravelMate_Backend.demo.model.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseUpdateRequest {
    
    @NotNull(message = "La descripción es obligatoria")
    private String description;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotNull(message = "La moneda es obligatoria")
    private Currency currency;

    @NotNull(message = "La fecha de compra es obligatoria")
    private LocalDate purchaseDate;
}

