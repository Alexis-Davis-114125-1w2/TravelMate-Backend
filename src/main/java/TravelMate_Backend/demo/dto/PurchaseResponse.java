package TravelMate_Backend.demo.dto;

import TravelMate_Backend.demo.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    private Long id;
    private Long tripId;
    private Long userId;
    private String userName;
    private String userEmail;
    private String description;
    private BigDecimal price;
    private Currency currency;
    private String currencySymbol;
    private LocalDate purchaseDate;
    private Boolean isGeneral;
    private Long createdBy; // ID del usuario que creó la compra
    private String createdByName; // Nombre del usuario que creó la compra
    private String createdByEmail; // Email del usuario que creó la compra
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

